package parrot

import ackcord.{ClientSettings, DiscordClient}
import akka.actor.typed.{ActorSystem, Behavior, DispatcherSelector}
import akka.actor.typed.scaladsl.Behaviors

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

object Guardian {
  sealed trait Message

  object Message {
    case class ClientCreated(client: DiscordClient) extends Message
    case class ClientCreationFailed(cause: Throwable) extends Message
  }

  def apply(clientSettings: ClientSettings): Behavior[Message] = Behaviors.setup[Message] { context =>
    implicit val system: ActorSystem[Nothing] = context.system
    implicit val ec: ExecutionContext = system.dispatchers.lookup(DispatcherSelector.default())

    clientSettings
      .createClient()
      .onComplete {
        case Failure(cause) =>
          context.self.tell(Message.ClientCreationFailed(cause))

        case Success(client) =>
          context.self.tell(Message.ClientCreated(client))
      }

    Behaviors.receiveMessage[Message] {
      case Message.ClientCreationFailed(cause) =>
        // @TODO exit code non-zero
        // @TODO log cause

        Behaviors.stopped

      case Message.ClientCreated(client) =>
        client.login()

        context.spawn(MessageReactor(client), "reactor")

        Behaviors.same
    }
  }
}