package parrot

import ackcord.{APIMessage, CacheState, DiscordClient}
import akka.actor.typed.{Behavior, PostStop}
import akka.actor.typed.scaladsl.Behaviors
import parrot.impls.{DailyThingSelector, GreetingTypeImpl}
import parrot.settings.ScheduledGreetingsSettings.GreetingContent
import parrot.settings.{ScheduledGreetingsSettings, Settings}

import java.time.{DayOfWeek, Instant}

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

        def behavior(
            maybeCacheState: Option[CacheState],
            fdfTakeover: Boolean
        ): Behavior[Message] =
          Behaviors
            .receiveMessage[Message] {
              case Message.Tick =>
                val now = Instant.now()
                val dayOfWeek =
                  now.atZone(DailyThingSelector.zoneId).getDayOfWeek
                val isFriday = dayOfWeek == DayOfWeek.FRIDAY

                var nextFdfTakeover = fdfTakeover

                for {
                  cacheState <- maybeCacheState
                  greetingTypeImpl <- greetingTypeImpls
                  message <- greetingTypeImpl.tick(now)
                } message match {
                  case GreetingContent.Image(rawUrl) =>
                    val url = greetingTypeImpl match {
                      case _: GreetingTypeImpl.CaliMorningImpl
                          if rawUrl == ScheduledGreetingsSettings.Images.FdfForeshadowMorning =>
                        nextFdfTakeover = true

                        rawUrl

                      case _: GreetingTypeImpl.CaliMorningImpl
                          if fdfTakeover && isFriday =>
                        // remain fdfTakeover until the evening

                        ScheduledGreetingsSettings.Images.FdfMorning

                      case _: GreetingTypeImpl.CaliEveningImpl
                          if fdfTakeover && isFriday =>
                        nextFdfTakeover = false

                        ScheduledGreetingsSettings.Images.FdfEvening

                      case _ =>
                        rawUrl
                    }

                    client.sendImageToActive(url)(
                      cacheState.current,
                      context.executionContext
                    )

                  case GreetingContent.Text(text) =>
                    client.sendTextToActive(text)(
                      cacheState.current,
                      context.executionContext
                    )
                }

                behavior(
                  maybeCacheState = maybeCacheState,
                  fdfTakeover = nextFdfTakeover
                )

              case Message.DiscordApiMessageReceived(message) =>
                behavior(
                  maybeCacheState = Some(message.cache),
                  fdfTakeover = fdfTakeover
                )
            }
            .receiveSignal {
              case (_, _: PostStop) =>
                registration.stop()

                Behaviors.same
            }

        behavior(maybeCacheState = None, fdfTakeover = false)
      }
    }
}
