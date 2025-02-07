package parrot

import ackcord.{ClientSettings, DiscordClient}
import akka.actor.CoordinatedShutdown
import akka.actor.typed.{ActorSystem, Behavior, DispatcherSelector, Terminated}
import akka.actor.typed.scaladsl.Behaviors
import com.typesafe.scalalogging.StrictLogging
import parrot.impls.GreetingTypeImpl.{CaliEveningImpl, CaliMorningImpl}
import parrot.settings.Settings

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

object Guardian extends StrictLogging {
  sealed trait Message

  object Message {
    case class ClientCreated(client: DiscordClient) extends Message
    case class ClientCreationFailed(cause: Throwable) extends Message
  }

  def apply(clientSettings: ClientSettings): Behavior[Message] =
    Behaviors.setup[Message] { context =>
      implicit val system: ActorSystem[Nothing] = context.system
      implicit val ec: ExecutionContext =
        system.dispatchers.lookup(DispatcherSelector.default())

      def failService(): Behavior[Message] = {
        CoordinatedShutdown(system).run(ShutdownReason.ServiceFailure)
        Behaviors.same
      }

      clientSettings
        .createClient()
        .onComplete {
          case Failure(cause) =>
            context.self.tell(Message.ClientCreationFailed(cause))

          case Success(client) =>
            context.self.tell(Message.ClientCreated(client))
        }

      Behaviors
        .receiveMessage[Message] {
          case Message.ClientCreationFailed(cause) =>
            logger.error("client creation failed", cause)

            failService()

          case Message.ClientCreated(client) =>
            client.login()

            val messageReactor = context.spawn(
              behavior = MessageReactor(client),
              "reactor"
            )

            val greetingScheduler = context.spawn(
              behavior = GreetingScheduler(
                client,
                List(
                  Some(Settings.scheduledGreetings.morningGreetings)
                    .filter(_.nonEmpty)
                    .map(new CaliMorningImpl(_)),
                  Some(Settings.scheduledGreetings.eveningGreetings)
                    .filter(_.nonEmpty)
                    .map(new CaliEveningImpl(_))
                ).flatten
              ),
              name = "greeting-scheduler"
            )

            context.watch(messageReactor)
            context.watch(greetingScheduler)

            Behaviors.same
        }
        .receiveSignal {
          case (_, Terminated(value)) =>
            logger.error(s"watched actor has terminated, actor=$value")

            failService()
        }
    }
}
