package parrot.dadjokes

import ackcord.DiscordClient
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.Behavior
import parrot.impls.DailyThingSelector

import java.time.DayOfWeek
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

object DadJoker {
  sealed trait Message

  object Message {
    case object Tick extends Message
  }

  def apply(
      client: DiscordClient,
      jokes: Seq[DadJoke],
      tickInterval: FiniteDuration
  ): Behavior[Message] =
    Behaviors.setup[Message] { context =>
      Behaviors.withTimers { timers =>
        implicit val ec: ExecutionContext = context.executionContext

        // daily, between 6-8pm pacific, select 3 jokes

        val jokeSelector = new DailyThingSelector(
          name = "dad-joker",
          things = jokes,
          startHour = 19,
          rangeHours = 2,
          selectionSize = 3
        )({ j =>
          DailyThingSelector.ThingDescriptor(
            days = Set(
              DayOfWeek.MONDAY,
              DayOfWeek.TUESDAY,
              DayOfWeek.WEDNESDAY,
              DayOfWeek.THURSDAY,
              DayOfWeek.FRIDAY,
              DayOfWeek.SATURDAY,
              DayOfWeek.SUNDAY
            ),
            show = j.call
          )
        })

        timers.startTimerAtFixedRate(Message.Tick, tickInterval)

        Behaviors
          .receiveMessage[Message] {
            case Message.Tick =>
              val selectedJokes = jokeSelector.tick()

              if (selectedJokes.nonEmpty) {
                context.spawnAnonymous(DadJokeExecutor(client, selectedJokes))
              }

              Behaviors.same
          }
      }
    }

}
