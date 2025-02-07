package parrot.dadjokes

import ackcord.{APIMessage, CacheSnapshot, DiscordClient}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{Behavior, PostStop}
import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

// @TODO shut down after XX minutes
object DadJokeExecutor extends StrictLogging {
  private final val NumBuellers = 4

  private val BuellerDelay = 5.minutes

  sealed trait Message

  object Message {
    case class DiscordCacheReceived(cacheSnapshot: CacheSnapshot)
        extends Message
    case class DiscordApiMessageReceived(message: APIMessage) extends Message
    case class ProcessJokes(jokes: Seq[DadJoke]) extends Message
    case class SendBueller(num: Int) extends Message
  }

  def apply(
      client: DiscordClient,
      initialJokes: Seq[DadJoke]
  ): Behavior[Message] =
    Behaviors.setup[Message] { context =>
      Behaviors.withTimers { timers =>
        implicit val ec: ExecutionContext = context.executionContext

        /** Sends a message to the relevant channel */
        def tellChannel(
            cacheSnapshot: CacheSnapshot,
            text: String
        ): Future[Unit] = {
          import parrot.RichDiscordClient
          client.sendTextToActive(text)(cacheSnapshot, context.executionContext)
        }

        val registration = client.onEventSideEffects { cache =>
          context.self.tell(Message.DiscordCacheReceived(cache))

          {
            case message =>
              context.self.tell(Message.DiscordApiMessageReceived(message))
          }
        }

        def waitForState(): Behavior[Message] = {
          def initialize(cache: CacheSnapshot): Unit = {
            for {
              _ <- tellChannel(cache, "yo, got some jokes for y'all")
              _ <- tellChannel(
                cache,
                "when you're ready to hear the punchline, reply with a question mark"
              )
              _ <- tellChannel(cache, "here we go!")
            } context.self.tell(Message.ProcessJokes(initialJokes))
          }

          Behaviors
            .receiveMessage[Message] {
              case Message.DiscordCacheReceived(cache) =>
                initialize(cache)

                tellCall(cache)

              case Message.DiscordApiMessageReceived(message) =>
                initialize(message.cache.current)

                tellCall(message.cache.current)

              case Message.ProcessJokes(_) =>
                logger.warn(
                  s"received unexpected ProcessJokes(..) while in waitForState"
                )

                Behaviors.same
              case Message.SendBueller(_) =>
                logger.warn(
                  s"received unexpected SendBueller(..) while in waitForState"
                )

                Behaviors.same
            }
        }

        def tellCall(cacheSnapshot: CacheSnapshot): Behavior[Message] =
          Behaviors
            .receiveMessage[Message] {
              case Message.ProcessJokes(jokes) if jokes.nonEmpty =>
                val joke = jokes.head
                val remainingJokes = jokes.tail

                tellChannel(cacheSnapshot, bold(joke.call))
                timers.startSingleTimer(Message.SendBueller(0), BuellerDelay)

                waitForInquiry(cacheSnapshot, joke, remainingJokes)

              case Message.ProcessJokes(_) =>
                tellChannel(
                  cacheSnapshot,
                  "that's all i got, see y'all tomorrow!"
                )
                Behaviors.stopped

              case Message.DiscordCacheReceived(cache) =>
                tellCall(cache)

              case Message.DiscordApiMessageReceived(message) =>
                tellCall(message.cache.current)

              case Message.SendBueller(n) =>
                logger.warn(
                  s"received unexpected SendBueller($n) while in tellCall"
                )

                Behaviors.same
            }
            .receiveSignal {
              case (_, _: PostStop) =>
                registration.stop()

                Behaviors.same
            }

        def waitForInquiry(
            cacheSnapshot: CacheSnapshot,
            joke: DadJoke,
            remainingJokes: Seq[DadJoke]
        ): Behavior[Message] = {
          Behaviors
            .receiveMessage[Message] {
              case Message.ProcessJokes(_) =>
                logger.warn(
                  "received unexpected ProcessJokes while in waitForInquiry"
                )

                Behaviors.same

              case Message.DiscordApiMessageReceived(
                    message: APIMessage.MessageCreate
                  ) =>
                if (isInquiry(message.message.content)) {
                  timers.cancelAll()

                  tellChannel(message.cache.current, italics(joke.response))
                    .onComplete { _ =>
                      context.self.tell(Message.ProcessJokes(remainingJokes))
                    }

                  tellCall(message.cache.current)
                } else {
                  waitForInquiry(message.cache.current, joke, remainingJokes)
                }

              case Message.DiscordCacheReceived(cache) =>
                waitForInquiry(cache, joke, remainingJokes)

              case Message.DiscordApiMessageReceived(message) =>
                waitForInquiry(message.cache.current, joke, remainingJokes)

              case Message.SendBueller(n) if n + 1 >= NumBuellers =>
                for {
                  _ <- tellChannel(cacheSnapshot, "fuck you all")
                  _ <- tellChannel(cacheSnapshot, italics(joke.response))
                } context.self.tell(Message.ProcessJokes(remainingJokes))

                tellCall(cacheSnapshot)

              case Message.SendBueller(n) =>
                tellChannel(cacheSnapshot, "bueller?")
                timers.startSingleTimer(
                  Message.SendBueller(n + 1),
                  BuellerDelay
                )

                Behaviors.same
            }
            .receiveSignal {
              case (_, _: PostStop) =>
                registration.stop()

                Behaviors.same
            }
        }

        waitForState()
      }

    }

  /** Determines if the message is an inquiry */
  private def isInquiry(rawText: String): Boolean = rawText.trim == "?"

  private def bold(str: String): String = s"**$str**"

  private def italics(str: String): String = s"*$str*"
}
