package parrot

import ackcord.{APIMessage, CacheState, DiscordClient}
import akka.actor.typed.{Behavior, PostStop}
import akka.actor.typed.scaladsl.Behaviors
import parrot.impls.GreetingTypeImpl
import parrot.settings.ScheduledGreetingsSettings.GreetingContent
import parrot.settings.Settings

object GreetingScheduler {
  sealed trait Message

  object Message {
    case object Tick extends Message
    case class DiscordApiMessageReceived(message: APIMessage) extends Message
  }

  def apply(
      client: DiscordClient,
      greetingTypeImpls: List[GreetingTypeImpl]
  ): Behavior[Message] =
    Behaviors.setup[Message] { context =>
      val registration = client.onEventSideEffectsIgnore {
        case message =>
          context.self.tell(Message.DiscordApiMessageReceived(message))
      }

      Behaviors.withTimers[Message] { timers =>
        timers.startTimerAtFixedRate(
          Message.Tick,
          Settings.scheduledGreetings.tickInterval
        )

        def behavior(maybeCacheState: Option[CacheState]): Behavior[Message] =
          Behaviors
            .receiveMessage[Message] {
              case Message.Tick =>
                for {
                  cacheState <- maybeCacheState
                  greetingTypeImpl <- greetingTypeImpls
                  message <- greetingTypeImpl.tick()
                } message match {
                  case GreetingContent.Image(url) =>
                    client.sendImageToActive(url)(cacheState.current)

                  case GreetingContent.Text(text) =>
                    client.sendTextToActive(text)(cacheState.current)
                }

                Behaviors.same

              case Message.DiscordApiMessageReceived(message) =>
                behavior(Some(message.cache))
            }
            .receiveSignal {
              case (_, _: PostStop) =>
                registration.stop()

                Behaviors.same
            }

        behavior(None)
      }
    }
}
