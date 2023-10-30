package parrot.settings

import java.time.DayOfWeek
import scala.concurrent.duration._

object ScheduledGreetingsSettings {
  sealed trait GreetingType

  object GreetingType {

    /** Randomly between 8 and 10am pacific, a single greeting is picked according to the weighted
      * greetings that are defined.
      */
    case class CaliMorning(days: Set[DayOfWeek]) extends GreetingType
  }

  sealed trait GreetingContent
  case object GreetingContent {
    case class Image(url: String) extends GreetingContent
    case class Text(text: String) extends GreetingContent
  }

  case class Greeting[T <: GreetingType](
      content: GreetingContent,
      greetingType: T
  )
}

class ScheduledGreetingsSettings {
  import ScheduledGreetingsSettings._

  private val EveryDay = Set(
    DayOfWeek.MONDAY,
    DayOfWeek.TUESDAY,
    DayOfWeek.WEDNESDAY,
    DayOfWeek.THURSDAY,
    DayOfWeek.FRIDAY,
    DayOfWeek.SATURDAY,
    DayOfWeek.SUNDAY
  )

  /** Each interval, a tick is generated. Each greeting type will be invoked for each tick, and produces
    * a list of reactions. The greeting types may be stateful and can assume they are being called from
    * a single thread.
    */
  val tickInterval: FiniteDuration = 1.second

  /** Defines the greetings for the application, organized by day or EveryDay. */
  val greetings: Seq[Greeting[_]] = Seq(
    // Every Day
    Seq(
      GreetingContent.Image(
        "https://media.tenor.com/_l9zt0EOc-sAAAAC/girls.gif"
      )
    ).map(Greeting(_, GreetingType.CaliMorning(EveryDay))),
    // Sundays
    Seq(
    ).map(Greeting(_, GreetingType.CaliMorning(Set(DayOfWeek.SUNDAY)))),
    // Mondays
    Seq(
    ).map(Greeting(_, GreetingType.CaliMorning(Set(DayOfWeek.MONDAY)))),
    // Tuesdays
    Seq(
    ).map(Greeting(_, GreetingType.CaliMorning(Set(DayOfWeek.TUESDAY)))),
    // Wednesdays
    Seq(
    ).map(Greeting(_, GreetingType.CaliMorning(Set(DayOfWeek.WEDNESDAY)))),
    // Thursdays
    Seq(
    ).map(Greeting(_, GreetingType.CaliMorning(Set(DayOfWeek.THURSDAY)))),
    // Fridays
    Seq(
    ).map(Greeting(_, GreetingType.CaliMorning(Set(DayOfWeek.FRIDAY)))),
    // Saturdays
    Seq(
    ).map(Greeting(_, GreetingType.CaliMorning(Set(DayOfWeek.SATURDAY))))
  ).flatten
}
