package parrot.ircbridge

import akka.Done
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl._
import akka.stream.{OverflowStrategy, QueueOfferResult}
import akka.stream.scaladsl.{Flow, Framing, Keep, Sink, Source, Tcp}
import akka.util.ByteString
import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.concurrent.impl.Promise
import scala.util.{Failure, Success}

object IrcBridge extends StrictLogging {
  // @TODO settings for these
  private val IrcUsernameAndNick = "theinvisiblehand"
  private val ReconnectInterval = 10.seconds

  sealed trait Message

  object Message {
    case object Start extends Message
    case object Stop extends Message

    /** I wish to receive all of your IRC messages that you've received */
    case class AddSubscriber(replyTo: ActorRef[CrapIrcProtocol.Incoming.ChannelPrivmsg]) extends Message
  }

  def apply(
      discordChannelId: Long,
      ircHost: String,
      ircPort: Int,
      ircChannel: String
  ): Behavior[Message] =
    Behaviors.setup { implicit context =>
      // @TODO hook into coordinated shutdown

      implicit val ec: ExecutionContext = context.executionContext
      implicit val system: ActorSystem[_] = context.system

      def encode(line: String): ByteString = ByteString(s"$line\r\n")

      def connect(): (Future[Tcp.OutgoingConnection], Future[Done]) = {
        val tcp = Tcp(context.system).outgoingConnection(ircHost, ircPort)

        val (outQueue, outSource) = Source
          .queue[ByteString](bufferSize = 10240, overflowStrategy = OverflowStrategy.backpressure)
          .preMaterialize()

        val protocol = Flow[ByteString]
          .via(
            Framing.delimiter(
              delimiter = ByteString("\r\n"),
              maximumFrameLength = 2048,
              allowTruncation = false
            )
          )
          .map(_.utf8String)
          .map { line =>
            val result = CrapIrcProtocol.Incoming.parse(line)

            logger.info(s"parsed line=$line into result=$result")

            result
          }
          .collect {
            case Some(
                  CrapIrcProtocol.Incoming.Welcome001(server, nick, text)
                ) =>
              logger.info(
                s"received the 001, server=$server, nick=$nick, text=$text"
              )

              Some(CrapIrcProtocol.Outgoing.Join(ircChannel))

            case Some(CrapIrcProtocol.Incoming.Ping(token1, token2)) =>
              Some(CrapIrcProtocol.Outgoing.Pong(token1, token2))

            case Some(CrapIrcProtocol.Incoming.ChannelPrivmsg(_, _, _)) =>
              // @TODO forward to discord

              None
          }
          .collect { case Some(outgoing) => encode(outgoing.render) }

        Source(
          List(
            CrapIrcProtocol.Outgoing.Nick(nick = IrcUsernameAndNick),
            CrapIrcProtocol.Outgoing.User(
              username = IrcUsernameAndNick,
              mode = "0",
              unused = "*",
              realname = IrcUsernameAndNick
            )
          )
        )
          .map(m => encode(m.render))
          .merge(outSource)
          .viaMat(tcp)(Keep.right)
          .via(protocol)
          .mapAsync(1)(outQueue.offer)
          .mapAsync(1) {
            case QueueOfferResult.Enqueued =>
              Future.successful(Done)
            case QueueOfferResult.Dropped =>
              Future.failed(new IllegalStateException("an element was dropped; check queue sizes"))
            case QueueOfferResult.Failure(cause) =>
              Future.failed(cause)
            case QueueOfferResult.QueueClosed =>
              Future.failed(new IllegalStateException("queue is closed"))
          }
          .toMat(Sink.ignore)(Keep.both)
          .run()
      }

      def scheduleReconnect(): Unit =
        context.scheduleOnce(ReconnectInterval, context.self, Message.Start)

      context.self ! Message.Start

      Behaviors.receiveMessage {
        case Message.Start =>
          val (connected, completed) = connect()

          connected.onComplete {
            case Success(connection) =>
              logger.info(
                s"connected to irc server, host=$ircHost, port=$ircPort, localAddress=${connection.localAddress} remoteAddress=${connection.remoteAddress}"
              )

            case Failure(cause) =>
              // do not reconnect here (connected) -- completed success/failure will take care of that
              logger.warn(
                s"disconnected from irc server, host=$ircHost, port=$ircPort",
                cause
              )
          }

          completed.onComplete {
            case Success(Done) =>
              logger.info(
                s"disconnected from irc server (successfully), reconnecting in a bit"
              )

              scheduleReconnect()

            case Failure(cause) =>
              logger.warn(
                s"disconnected from irc server (unsuccessfully), reconnecting in a bit",
                cause
              )

              scheduleReconnect()
          }

          Behaviors.same

        case Message.Stop =>
          Behaviors.stopped

        case Message.Forward(message) =>


          // @TODO track the current outQueue, send this there

          Behaviors.same
      }
    }
}
