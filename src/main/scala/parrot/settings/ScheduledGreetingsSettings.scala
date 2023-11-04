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
    Seq[String](
      //"https://media.tenor.com/_l9zt0EOc-sAAAAC/girls.gif"
    ).map(url =>
      Greeting(
        GreetingContent.Image(url),
        GreetingType.CaliMorning(EveryDay)
      )
    ),
    // Sundays
    Seq(
      "https://img1.picmix.com/output/pic/normal/9/4/1/6/11096149_a194d.gif",
      "https://i.giphy.com/media/62Xg1C7ttZyZpVSU9C/giphy.webp"
    ).map(url =>
      Greeting(
        GreetingContent.Image(url),
        GreetingType.CaliMorning(Set(DayOfWeek.SUNDAY))
      )
    ),
    // Mondays
    Seq(
      "https://media.tenor.com/VVfhXRyHEF4AAAAC/monday-left-me-broken-cat.gif",
      "https://i.giphy.com/media/tqj4m9BRURayxQAIW9/giphy.webp",
      "https://i.giphy.com/media/l0MYxP6UQvojorkoo/giphy.webp"
    ).map(url =>
      Greeting(
        GreetingContent.Image(url),
        GreetingType.CaliMorning(Set(DayOfWeek.MONDAY))
      )
    ),
    // Tuesdays
    Seq(
      "https://i.giphy.com/media/hcv7WXFlfys9q0XJTq/giphy.webp",
      "https://i.giphy.com/media/flL6zRWgnNDvSidTcX/giphy.webp",
      "https://i.giphy.com/media/C63ZDiuirVuRO2QzK4/giphy.webp",
      "https://i.giphy.com/media/8hslg9Pba3Uoq3U9i1/giphy.webp"
    ).map(url =>
      Greeting(
        GreetingContent.Image(url),
        GreetingType.CaliMorning(Set(DayOfWeek.TUESDAY))
      )
    ),
    // Wednesdays
    Seq(
      "https://i.giphy.com/media/uLgd9dOYWpnu5WkShY/giphy.webp",
      "https://i.giphy.com/media/YjKWzP8n97YisqCWPK/giphy.webp",
      "https://media.tenor.com/p1ME7s6S_7sAAAAC/have-an-awesome-wednesday-stay-safe-and-blessed.gif",
      "https://img1.picmix.com/output/pic/normal/9/8/0/5/10935089_13a7f.gif",
      "https://img1.picmix.com/output/pic/normal/7/4/2/2/6592247_181a4.gif",
      "https://img1.picmix.com/output/pic/normal/7/8/8/7/11397887_80bdb.gif"
    ).map(url =>
      Greeting(
        GreetingContent.Image(url),
        GreetingType.CaliMorning(Set(DayOfWeek.WEDNESDAY))
      )
    ),
    // Thursdays
    Seq(
      "https://img1.picmix.com/output/pic/normal/8/7/1/5/11065178_974ee.gif",
      "https://img1.picmix.com/output/pic/normal/4/5/2/8/10758254_b822a.gif",
      "https://img1.picmix.com/output/pic/normal/6/2/0/1/4021026_762ea.gif",
      "https://img1.picmix.com/output/pic/normal/3/3/4/9/9869433_e92f9.gif"
    ).map(url =>
      Greeting(
        GreetingContent.Image(url),
        GreetingType.CaliMorning(Set(DayOfWeek.THURSDAY))
      )
    ),
    // Fridays
    Seq(
      "https://media.tenor.com/3pS3gpKJsLIAAAAC/carlton-dance.gif",
      "https://media.tenor.com/Zf45U-rHMgkAAAAd/friday-good-morning.gif",
      "https://www.ludlowcub.com/wp-content/uploads/2011/04/rebecca-black-friday.jpg",
      "https://img1.picmix.com/output/pic/normal/3/5/4/6/11376453_150ef.gif",
      "https://img1.picmix.com/output/pic/normal/6/7/0/8/10848076_3dc0f.gif"
    ).map(url =>
      Greeting(
        GreetingContent.Image(url),
        GreetingType.CaliMorning(Set(DayOfWeek.FRIDAY))
      )
    ),
    // Saturdays
    Seq(
      "https://media.tenor.com/R3H7o7xRfUMAAAAC/it%27s-saturday.gif"
    ).map(url =>
      Greeting(
        GreetingContent.Image(url),
        GreetingType.CaliMorning(Set(DayOfWeek.SATURDAY))
      )
    )
  ).flatten
}
