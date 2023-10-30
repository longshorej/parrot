package parrot

import ackcord.{APIMessage, CacheSnapshot, DiscordClient}
import ackcord.data.{MessageId, RawSnowflake, TextChannelId}
import ackcord.requests.{CreateMessage, CreateMessageData}
import ackcord.syntax.MessageSyntax
import akka.actor.typed.{Behavior, PostStop}
import akka.actor.typed.scaladsl.Behaviors
import parrot.logic.evaluateWordle.Status
import parrot.logic.{evaluateWordle, getReactions}
import parrot.settings.Settings

import scala.concurrent.ExecutionContext
import scala.util.Random

object MessageReactor {
  assert(Settings.wordle.words.nonEmpty)

  sealed trait Message

  object Message {
    case class DiscordApiMessageReceived(message: APIMessage) extends Message
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

  def apply(client: DiscordClient): Behavior[Message] =
    handle(
      client = client,
      waiting = Set.empty,
      active = true,
      maybeWordle = None,
      last = None
    )

  // @TODO active should be per server, not per instance
  private def handle(
      client: DiscordClient,
      waiting: Set[MessageId],
      active: Boolean,
      maybeWordle: Option[WordleGame],
      last: Option[String]
  ): Behavior[Message] = {
    Behaviors.setup[Message] { context =>
      implicit val ec: ExecutionContext = context.executionContext

      val registration = client.onEventSideEffectsIgnore {
        case message =>
          context.self.tell(Message.DiscordApiMessageReceived(message))
      }

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
            handle(
              client = client,
              waiting = waiting - message.message.id,
              active = active,
              maybeWordle = maybeWordle,
              last = last
            )

          case Message.DiscordApiMessageReceived(
                message: APIMessage.MessageCreate
              ) =>
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

                handle(
                  client = client,
                  waiting = waiting + message.message.id,
                  active = active,
                  maybeWordle = maybeWordle,
                  last = Some(message.message.id.asString)
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

                    handle(
                      client = client,
                      waiting = waiting,
                      active = true,
                      maybeWordle = maybeWordle,
                      last = Some(message.message.id.asString)
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

                    handle(
                      client = client,
                      waiting = waiting,
                      active = false,
                      maybeWordle = maybeWordle,
                      last = Some(message.message.id.asString)
                    )

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
                      message.cache.current
                    )

                    handle(
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
                      last = Some(message.message.id.asString)
                    )

                  case CrapProtocol.WordleHint
                      if maybeWordle.nonEmpty && maybeWordle.get.authorUsername == message.message.authorUsername =>
                    val wordle = maybeWordle.get
                    val index = Random.nextInt(evaluateWordle.GuessLimit)

                    client.sendTextToActive(
                      s"letter #${index + 1} is ${wordle.word.lift(index).fold("")(_.toString)}"
                    )(message.cache.current)

                    handle(
                      client = client,
                      waiting = waiting,
                      active = active,
                      maybeWordle = maybeWordle,
                      last = Some(message.message.id.asString)
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
                        message.cache.current
                      )

                      handle(
                        client = client,
                        waiting = waiting,
                        active = active,
                        maybeWordle = None,
                        last = Some(message.message.id.asString)
                      )
                    } else if (guesses.length == evaluateWordle.GuessLimit) {
                      client.sendTextToActive(
                        s"god ur bad (it was ${wordle.word})"
                      )(
                        message.cache.current
                      )

                      handle(
                        client = client,
                        waiting = waiting,
                        active = active,
                        maybeWordle = None,
                        last = Some(message.message.id.asString)
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
                        message.cache.current
                      )
                      // @TODO improve the format

                      handle(
                        client = client,
                        waiting = waiting,
                        active = active,
                        maybeWordle = Some(wordle.copy(guesses = guesses)),
                        last = Some(message.message.id.asString)
                      )
                    }

                  case _ =>
                    Behaviors.same
                }

              case _ =>
                // @TODO why are we getting dups
                Behaviors.same
            }

          case Message.DiscordApiMessageReceived(_) =>
            Behaviors.same
        }
        .receiveSignal {
          case (_, _: PostStop) =>
            registration.stop()

            Behaviors.same
        }
    }
  }
}
