package parrot

import ackcord.{APIMessage, CacheSnapshot, DiscordClient, EventRegistration}
import ackcord.data.{GuildChannel, GuildGatewayMessage, MessageId, SparseMessage}
import ackcord.syntax.MessageSyntax
import akka.NotUsed
import akka.actor.typed.{ActorRef, Behavior, PostStop}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.http.caching.LfuCache
import akka.http.caching.scaladsl.CachingSettings
import com.typesafe.scalalogging.StrictLogging
import parrot.logic.evaluateWordle.Status
import parrot.logic.{evaluateWordle, getReactions}
import parrot.settings.Settings

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random
import scala.concurrent.duration._
import scala.util.chaining._
object MessageReactor extends StrictLogging {
  assert(Settings.wordle.words.nonEmpty)

  private val BotUsername = "the-invisible-hand"

  sealed trait Message

  object Message {
    case class DiscordApiMessageReceived(message: APIMessage) extends Message
    case class IssueReceived(message: String) extends Message
    case class Subscribe(replyTo: String => Unit) extends Message
    private[MessageReactor] case class Reacted(
        message: APIMessage.MessageMessage,
        remaining: List[String]
    ) extends Message

    private[MessageReactor] case object WordleTimedOut
  }

  private case class WordleGame(
      authorUsername: String,
      word: String,
      guesses: Seq[Vector[evaluateWordle.Status]]
  )

  def apply(
      greetingScheduler: ActorRef[GreetingScheduler.Message],
      client: DiscordClient
  ): Behavior[Message] =
    Behaviors.setup[Message] { implicit context =>
      implicit val ec: ExecutionContext = context.executionContext

      val seenCache = LfuCache[Long, Unit](
        CachingSettings(context.system)
          .pipe(settings =>
            settings.withLfuCacheSettings(
              settings.lfuCacheSettings
                .withInitialCapacity(1024)
                .withMaxCapacity(8192)
                .withTimeToLive(1.minute)
                .withTimeToIdle(30.seconds)
            )
          )
      )

      //client.events.subscribe.runForeach()

      val registration = client.onEventSideEffectsIgnore {
        case message =>
          context.self.tell(Message.DiscordApiMessageReceived(message))
      }

      //client.events.subscribe.runForeach()

      def running(
          greetingScheduler: ActorRef[GreetingScheduler.Message],
          client: DiscordClient,
          waiting: Set[MessageId],
          active: Boolean,
          maybeWordle: Option[WordleGame],
          last: Option[String],
          subscribers: List[String => Unit],
          cacheSnapshot: Option[CacheSnapshot]
      )(implicit
          context: ActorContext[Message],
          ec: ExecutionContext
      ): Behavior[Message] = {
        // @TODO does ackord do some client side inspection of event stream? why is coordinating on future
        // @TODO resolution enough?! i'd have expected to do that myself

        Behaviors
          .receiveMessagePartial[Message] {
            case Message.Reacted(message, reaction :: remaining) =>
              context.log.info(
                s"cont - id=${message.message.id} remaining=$remaining"
              )

              // @TODO future failure
              client.requestsHelper
                .run(message.message.createReaction(reaction))(
                  message.cache.current
                )
                .foreach { _ =>
                  context.self ! Message.Reacted(message, remaining)
                }

              Behaviors.same

            case Message.Reacted(message, Nil) =>
              running(
                greetingScheduler = greetingScheduler,
                client = client,
                waiting = waiting - message.message.id,
                active = active,
                maybeWordle = maybeWordle,
                last = last,
                subscribers = subscribers,
                cacheSnapshot = Some(message.cache.current)
              )

            case Message.DiscordApiMessageReceived(
                  message: APIMessage.MessageCreate
                ) =>

              val messageId = message.message.id.toUnsignedLong
              val channelId = message.message.channelId

              def formatUsername(username: String)(implicit c: CacheSnapshot): String =
                message.message match {
                  case sparseMessage: SparseMessage =>
                    

                  case guildGatewayMessage: GuildGatewayMessage =>
                    (for {
                      guildMember <- guildGatewayMessage.guildMember
                      nick <- guildMember.nick
                    } yield nick).getOrElse(message.message.authorUsername)
                }
              {
                val username = (for {
                  cache <- cacheSnapshot
                  guildMember <- message.message.guildMember(cache)
                  nick <- guildMember.nick
                } yield nick).getOrElse(message.message.authorUsername)


              }

              def formatMentions(message: GuildGatewayMessage)
                                 (implicit c: CacheSnapshot): String = {
                // extracted from upstream source, changes user resolution to consider server nickname

                val userList = message.mentions.toList.flatMap(_.resolve)
                val roleList = message.mentionRoles.toList.flatMap(_.resolve)
                val optGuildId = channelId.resolve.collect {
                  case channel: GuildChannel => channel.guildId
                }
                val channelList =
                  optGuildId.fold[List[Option[GuildChannel]]](Nil)(guildId => message.channelMentions.toList.map(_.resolve(guildId)))

                val withUsers = userList
                  .foldRight(message.content)((user, content) => content.replace(user.mention, s"@${user.username}"))
                val withRoles = roleList
                  .foldRight(withUsers)((role, content) => content.replace(role.mention, s"@${role.name}"))
                val withChannels = channelList.flatten
                  .foldRight(withRoles)((channel, content) => content.replace(channel.mention, s"@${channel.name}"))

                withChannels
              }


              seenCache.getOrLoad(
                messageId,
                { _ =>
                  if (
                    message.message.authorUsername != BotUsername && message.message.channelId.toUnsignedLong == Settings.textChannelId
                  ) {
                    val content =
                      if (
                        message.message.content.isEmpty && message.message.attachment.nonEmpty
                      )
                        message.message.attachment.map(_.url)
                      else {
                        val content = (
                        for {
                          cache <- cacheSnapshot
                          content = message.message.formatMentions(cache)
                        } yield content).getOrElse(message.message.content)

                        List(content)
                      }

                    val username = (for {
                      cache <- cacheSnapshot
                      guildMember <- message.message.guildMember(cache)
                      nick <- guildMember.nick
                    } yield nick).getOrElse(message.message.authorUsername)

                    content.foreach { c =>
                      val formatted =
                        s"<$username> $c"

                      subscribers.foreach(_.apply(formatted))
                    }

                  }

                  Future.successful(())
                }
              )

              // @TODO need something more composable here - this trends toward italian noodle
              getReactions(message.message.content) match {
                case reaction :: reactions
                    if active && !waiting.contains(message.message.id) =>
                  context.log.info(
                    s"init - id=${message.message.id} reaction=$reaction remaining=$reactions"
                  )

                  // @TODO future failure
                  client.requestsHelper
                    .run(message.message.createReaction(reaction))(
                      message.cache.current
                    )
                    .foreach { _ =>
                      context.self ! Message.Reacted(message, reactions)
                    }

                  running(
                    greetingScheduler = greetingScheduler,
                    client = client,
                    waiting = waiting + message.message.id,
                    active = active,
                    maybeWordle = maybeWordle,
                    last = Some(message.message.id.asString),
                    subscribers = subscribers,
                    cacheSnapshot = Some(message.cache.current)
                  )

                case _ if !last.contains(message.message.id.asString) =>
                  // @TODO duplicates

                  context.log.info("received message id={}", message.message.id)
                  message.message.content match {
                    case CrapProtocol.Start =>
                      // @TODO future failure
                      client.requestsHelper
                        .run(
                          message.message
                            .createReaction(CrapProtocol.StartResponse)
                        )(
                          message.cache.current
                        )

                      running(
                        greetingScheduler = greetingScheduler,
                        client = client,
                        waiting = waiting,
                        active = true,
                        maybeWordle = maybeWordle,
                        last = Some(message.message.id.asString),
                        subscribers = subscribers,
                        cacheSnapshot = Some(message.cache.current)
                      )

                    case CrapProtocol.Stop =>
                      // @TODO future failure
                      client.requestsHelper
                        .run(
                          message.message
                            .createReaction(CrapProtocol.StopResponse)
                        )(
                          message.cache.current
                        )

                      running(
                        greetingScheduler = greetingScheduler,
                        client = client,
                        waiting = waiting,
                        active = false,
                        maybeWordle = maybeWordle,
                        last = Some(message.message.id.asString),
                        subscribers = subscribers,
                        cacheSnapshot = Some(message.cache.current)
                      )

                    case CrapProtocol.KeepOnRolling =>
                      client.requestsHelper
                        .run(
                          message.message
                            .createReaction(CrapProtocol.KeepOnRollingResponse)
                        )(
                          message.cache.current
                        )
                      greetingScheduler ! GreetingScheduler.Message.KeepOnRolling
                      Behaviors.same

                    case CrapProtocol.WordleNew if maybeWordle.isEmpty =>
                      // @TODO schedule a timeout
                      // @TODO wordlegame needs an id - to ensure timeout applied correctly
                      val word =
                        Settings.wordle.words(
                          Random.nextInt(Settings.wordle.words.length)
                        )

                      context.log.info(
                        "starting new wordle game, authorId={} authorUsername={} word={}",
                        message.message.authorId.asString,
                        message.message.authorUsername,
                        word
                      )

                      client.sendTextToActive("starting new wordle game")(
                        message.cache.current,
                        context.executionContext
                      )

                      running(
                        greetingScheduler = greetingScheduler,
                        client = client,
                        waiting = waiting,
                        active = active,
                        maybeWordle = Some(
                          WordleGame(
                            authorUsername = message.message.authorUsername,
                            word = word,
                            guesses = Vector.empty
                          )
                        ),
                        last = Some(message.message.id.asString),
                        subscribers = subscribers,
                        cacheSnapshot = Some(message.cache.current)
                      )

                    case CrapProtocol.WordleHint
                        if maybeWordle.nonEmpty && maybeWordle.get.authorUsername == message.message.authorUsername =>
                      val wordle = maybeWordle.get
                      val index = Random.nextInt(evaluateWordle.GuessLimit)

                      client.sendTextToActive(
                        s"letter #${index + 1} is ${wordle.word.lift(index).fold("")(_.toString)}"
                      )(message.cache.current, context.executionContext)

                      running(
                        greetingScheduler = greetingScheduler,
                        client = client,
                        waiting = waiting,
                        active = active,
                        maybeWordle = maybeWordle,
                        last = Some(message.message.id.asString),
                        subscribers = subscribers,
                        cacheSnapshot = Some(message.cache.current)
                      )
                    case line
                        if line.startsWith(
                          CrapProtocol.WordleGuessPrefix
                        ) && maybeWordle.nonEmpty && maybeWordle.get.authorUsername == message.message.authorUsername =>
                      // @TODO require dictionary check
                      val wordle = maybeWordle.get // see maybeWordle.nonEmpty
                      val guess =
                        line.drop(CrapProtocol.WordleGuessPrefix.length).trim
                      context.log.info(
                        "received wordle guess={} for word={} for authorUsername={} from authorUsername={}",
                        guess,
                        wordle.word,
                        wordle.authorUsername,
                        message.message.authorUsername
                      )
                      val result = evaluateWordle(guess, wordle.word)
                      val guesses = wordle.guesses :+ result

                      if (result.forall(_ == evaluateWordle.Status.Correct)) {
                        client.sendTextToActive("you win, feels good man")(
                          message.cache.current,
                          context.executionContext
                        )

                        running(
                          greetingScheduler = greetingScheduler,
                          client = client,
                          waiting = waiting,
                          active = active,
                          maybeWordle = None,
                          last = Some(message.message.id.asString),
                          subscribers = subscribers,
                          cacheSnapshot = Some(message.cache.current)
                        )
                      } else if (guesses.length == evaluateWordle.GuessLimit) {
                        client.sendTextToActive(
                          s"god ur bad (it was ${wordle.word})"
                        )(
                          message.cache.current,
                          context.executionContext
                        )

                        running(
                          greetingScheduler = greetingScheduler,
                          client = client,
                          waiting = waiting,
                          active = active,
                          maybeWordle = None,
                          last = Some(message.message.id.asString),
                          subscribers = subscribers,
                          cacheSnapshot = Some(message.cache.current)
                        )
                      } else {
                        val formattedResult = result
                          .map {
                            case Status.Correct   => "\uD83D\uDFE9"
                            case Status.InWord    => "\uD83D\uDFE8"
                            case Status.NotInWord => "\uD83D\uDFE5"
                          }
                          .mkString("")

                        client.sendTextToActive(formattedResult)(
                          message.cache.current,
                          context.executionContext
                        )
                        // @TODO improve the format

                        running(
                          greetingScheduler = greetingScheduler,
                          client = client,
                          waiting = waiting,
                          active = active,
                          maybeWordle = Some(wordle.copy(guesses = guesses)),
                          last = Some(message.message.id.asString),
                          subscribers = subscribers,
                          cacheSnapshot = Some(message.cache.current)
                        )
                      }

                    case _ =>
                      Behaviors.same
                  }

                case _ =>
                  // @TODO why are we getting dups
                  Behaviors.same
              }

            case Message.Subscribe(subscriber) =>
              running(
                greetingScheduler = greetingScheduler,
                client = client,
                waiting = waiting,
                active = active,
                maybeWordle = maybeWordle,
                last = last,
                subscribers = subscriber :: subscribers,
                cacheSnapshot = cacheSnapshot
              )

            case Message.IssueReceived(m) =>
              if (cacheSnapshot.isEmpty) {
                logger.warn(
                  "cache snapshot is empty when attempting to send an issue"
                )
              }

              // @TODO how to always have cache so we dont need to wait for a message?
              cacheSnapshot.foreach { implicit cache =>
                client.sendTextToActive(m)
              }

              Behaviors.same

            case Message.DiscordApiMessageReceived(_) =>
              Behaviors.same
          }
          .receiveSignal {
            case (_, _: PostStop) =>
              registration.stop()

              Behaviors.same
          }

      }

      // @TODO active should be per server, not per instance
      running(
        greetingScheduler = greetingScheduler,
        client = client,
        waiting = Set.empty,
        active = true,
        maybeWordle = None,
        last = None,
        subscribers = List.empty,
        cacheSnapshot = None
      )
    }
}
