package parrot

import ackcord.{APIMessage, DiscordClient}
import ackcord.data.{MessageId, TextChannelId}
import ackcord.requests.{CreateMessage, CreateMessageData}
import ackcord.syntax.MessageSyntax
import akka.actor.typed.{Behavior, PostStop}
import akka.actor.typed.scaladsl.Behaviors
import parrot.logic.getReactions

import scala.concurrent.ExecutionContext

object MessageReactor {
  sealed trait Message

  object Message {
    case class DiscordApiMessageReceived(message: APIMessage) extends Message
    private[MessageReactor] case class Reacted(
        message: APIMessage.MessageMessage,
        remaining: List[String]
    ) extends Message
  }

  def apply(client: DiscordClient): Behavior[Message] =
    handle(
      client = client,
      waiting = Set.empty,
      active = true,
      wordle = None
    )

  // @TODO active should be per server, not per instance
  private def handle(
      client: DiscordClient,
      waiting: Set[MessageId],
      active: Boolean,
      wordle: Option[String]
  ): Behavior[Message] =
    Behaviors.setup[Message] { context =>
      implicit val ec: ExecutionContext = context.executionContext

      val registration = client.onEventSideEffectsIgnore {
        case message =>
          context.self.tell(Message.DiscordApiMessageReceived(message))
      }

      //client.requestsHelper.run(CreateMessage(TextChannelId(826348192084787231L), CreateMessageData("test")))(message.cache.current)

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
              wordle = wordle
            )

          case Message.DiscordApiMessageReceived(
                message: APIMessage.MessageMessage
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
                  wordle = wordle
                )

              case _ =>
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
                      wordle = wordle
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
                      wordle = wordle
                    )

                  case CrapProtocol.WordleNew =>
                    Behaviors.same

                  case _ =>
                    Behaviors.same
                }
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
