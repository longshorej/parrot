package parrot

import ackcord.{APIMessage, DiscordClient}
import ackcord.data.MessageId
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
      waiting = Set.empty
    )

  private def handle(
      client: DiscordClient,
      waiting: Set[MessageId]
  ): Behavior[Message] =
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
            handle(client, waiting - message.message.id)

          case Message.DiscordApiMessageReceived(
                message: APIMessage.MessageMessage
              ) =>
            if (
              message.message.reactions.isEmpty && !waiting.contains(
                message.message.id
              )
            ) {
              val reactions = getReactions(message.message.content)

              reactions match {
                case reaction :: reactions =>
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

                  handle(client, waiting + message.message.id)

                case Nil =>
                  Behaviors.same
              }
            } else {
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
