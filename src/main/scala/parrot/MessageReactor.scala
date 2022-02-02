package parrot

import ackcord.{APIMessage, DiscordClient}
import ackcord.data.MessageId
import ackcord.syntax.MessageSyntax
import akka.actor.typed.{Behavior, PostStop}
import akka.actor.typed.scaladsl.Behaviors
import parrot.logic.getReactions
import scala.concurrent.ExecutionContext

/**
 * An actor behavior that manages reacting to messages
 */
object MessageReactor {
  sealed trait Message

  object Message {
    case class DiscordApiMessageReceived(message: APIMessage) extends Message
    private[MessageReactor] case class Reacted(message: APIMessage.MessageMessage, remaining: List[String]) extends Message
  }

  def apply(client: DiscordClient): Behavior[Message] = handle(
    client = client,
    waiting = Map.empty
  )

  private def handle(client: DiscordClient, waiting: Map[MessageId, List[String]]): Behavior[Message] = Behaviors.setup[Message] { context =>
    implicit val ec: ExecutionContext = context.executionContext

    val registration = client.onEventSideEffectsIgnore {
      case message => context.self.tell(Message.DiscordApiMessageReceived(message))
    }

    Behaviors
      .receiveMessagePartial[Message] {
        case Message.DiscordApiMessageReceived(reactionAdd: APIMessage.MessageReactionAdd) =>
          // @TODO actually check if it was our reaction

          Behaviors.same

        case Message.Reacted(message, reaction :: remaining) =>
          context.log.info(s"cont - id=${message.message.id} remaining=$remaining")

          client.requestsHelper
            .run(message.message.createReaction(reaction))(message.cache.current)
            .foreach { _ =>
              context.self ! Message.Reacted(message, remaining)
            }

          Behaviors.same

        case Message.Reacted(_, Nil) =>
          Behaviors.same

        case Message.DiscordApiMessageReceived(message: APIMessage.MessageMessage) =>
          if (message.message.reactions.isEmpty && !waiting.contains(message.message.id)) {
            val reactions = getReactions(message.message.content)

            reactions match {
              case reaction :: reactions =>
                context.log.info(s"init - id=${message.message.id} reaction=$reaction remaining=$reactions")
                client.requestsHelper
                  .run(message.message.createReaction(reaction))(message.cache.current)
                  .foreach { _ =>
                    context.self ! Message.Reacted(message, reactions)
                  }

                Behaviors.same

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