package parrot.settings

import java.time.DayOfWeek
import scala.concurrent.duration._

object ScheduledGreetingsSettings {
  sealed trait GreetingType {
    def days: Set[DayOfWeek]
  }

  object GreetingType {

    /** Randomly between 8am and 10am pacific, a single greeting is picked according to the weighted
      * greetings that are defined.
      */
    case class CaliMorning(override val days: Set[DayOfWeek])
        extends GreetingType

    /** Randomly between 11pm and 1am pacific, a single greeting is picked according to the weighted
      * greetings that are defined.
      */
    case class CaliEvening(override val days: Set[DayOfWeek])
        extends GreetingType
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

  /** Defines the morning greetings for the application, organized by day or EveryDay. */
  val morningGreetings: Seq[Greeting[GreetingType.CaliMorning]] = Seq(
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
      "https://i.giphy.com/media/62Xg1C7ttZyZpVSU9C/giphy.gif"
    ).map(url =>
      Greeting(
        GreetingContent.Image(url),
        GreetingType.CaliMorning(Set(DayOfWeek.SUNDAY))
      )
    ),
    // Mondays
    Seq(
      "https://media.tenor.com/VVfhXRyHEF4AAAAC/monday-left-me-broken-cat.gif",
      "https://i.giphy.com/media/tqj4m9BRURayxQAIW9/giphy.gif",
      "https://i.giphy.com/media/l0MYxP6UQvojorkoo/giphy.gif",
      "https://i.pinimg.com/originals/4a/93/01/4a9301659a81911f23e86dc1e7509538.gif",
      "https://media.tenor.com/00hqr9WPHeQAAAAC/michael-j.gif",
      "https://media.tenor.com/GkNDPEh5hlMAAAAC/monday-motivation.gif"
    ).map(url =>
      Greeting(
        GreetingContent.Image(url),
        GreetingType.CaliMorning(Set(DayOfWeek.MONDAY))
      )
    ),
    // Tuesdays
    Seq(
      "https://i.giphy.com/media/hcv7WXFlfys9q0XJTq/giphy.gif",
      "https://i.giphy.com/media/flL6zRWgnNDvSidTcX/giphy.gif",
      "https://i.giphy.com/media/C63ZDiuirVuRO2QzK4/giphy.gif",
      "https://i.giphy.com/media/8hslg9Pba3Uoq3U9i1/giphy.gif"
    ).map(url =>
      Greeting(
        GreetingContent.Image(url),
        GreetingType.CaliMorning(Set(DayOfWeek.TUESDAY))
      )
    ),
    // Wednesdays
    Seq(
      "https://i.giphy.com/media/uLgd9dOYWpnu5WkShY/giphy.gif",
      "https://i.giphy.com/media/YjKWzP8n97YisqCWPK/giphy.gif",
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

  /** Defines the evening greetings for the application, organized by day or EveryDay. */
  val eveningGreetings: Seq[Greeting[GreetingType.CaliEvening]] = Seq(
    // Every Day
    Seq(
      "https://www.jasonlongshore.com/parrot/images/greetings/410714209-d8411d44-08a5-4702-acbd-7b34f057ed91.gif",
      "https://www.jasonlongshore.com/parrot/images/greetings/410714209-d8411d44-08a5-4702-acbd-7b34f057ed91.gif",
      "https://www.jasonlongshore.com/parrot/images/greetings/410714471-8287d0f5-ac04-4512-a478-132daa1b851c.gif",
      "https://www.jasonlongshore.com/parrot/images/greetings/410717054-7fb6134f-6f9f-4448-9d56-617ded0883e3.gif",
      "https://www.jasonlongshore.com/parrot/images/greetings/410717195-22ae9ae3-a66f-4908-a9ab-c237d3671433.gif",
      "https://www.jasonlongshore.com/parrot/images/greetings/410718174-9f2f1022-a522-4399-8f65-42c01a541c04.gif",
      "https://www.jasonlongshore.com/parrot/images/greetings/410718464-2e43428e-3817-4c6c-b5d9-a1e4149380ec.gif",
      "https://www.jasonlongshore.com/parrot/images/greetings/410718695-d0cb84ef-4e37-4196-aa3f-8178eebe375a.gif",
      "https://www.jasonlongshore.com/parrot/images/greetings/410718801-759c9ab2-ff2c-4026-855a-2f5404e51062.gif",
      "https://www.jasonlongshore.com/parrot/images/greetings/410718858-7740214a-4833-43cf-8263-eb969d2601b8.gif",
      "https://www.jasonlongshore.com/parrot/images/greetings/410719096-f48571e1-98bd-4012-9104-3319bf183764.gif",
      "https://www.jasonlongshore.com/parrot/images/greetings/410719216-c4527769-e5bf-4b1c-9fe9-b3fc9c35b044.gif",
      "https://www.jasonlongshore.com/parrot/images/greetings/410719416-5591c68c-a834-4ad6-a5d0-3842089afc75.gif",
      "https://www.jasonlongshore.com/parrot/images/greetings/410719651-6a5f6097-ff31-4a8c-8fbd-732933880c20.gif",
      "https://www.jasonlongshore.com/parrot/images/greetings/410719708-66b8a82e-6cff-43da-8d7a-eaaf75298150.gif",
      "https://www.jasonlongshore.com/parrot/images/greetings/410720452-76898abb-263c-44c1-a950-37c2e0f8fc66.gif"
    ).map(url =>
      Greeting(
        GreetingContent.Image(url),
        GreetingType.CaliEvening(EveryDay)
      )
    ),
    // Sundays
    Seq(
      // "http://urlhere.gif"
    ).map(url =>
      Greeting(
        GreetingContent.Image(url),
        GreetingType.CaliEvening(Set(DayOfWeek.SUNDAY))
      )
    ),
    // Mondays
    Seq(
      // "http://urlhere.gif"
    ).map(url =>
      Greeting(
        GreetingContent.Image(url),
        GreetingType.CaliEvening(Set(DayOfWeek.MONDAY))
      )
    ),
    // Tuesdays
    Seq(
      // "http://urlhere.gif"
    ).map(url =>
      Greeting(
        GreetingContent.Image(url),
        GreetingType.CaliEvening(Set(DayOfWeek.TUESDAY))
      )
    ),
    // Wednesdays
    Seq(
      // "http://urlhere.gif"
    ).map(url =>
      Greeting(
        GreetingContent.Image(url),
        GreetingType.CaliEvening(Set(DayOfWeek.WEDNESDAY))
      )
    ),
    // Thursdays
    Seq(
      // "http://urlhere.gif"
    ).map(url =>
      Greeting(
        GreetingContent.Image(url),
        GreetingType.CaliEvening(Set(DayOfWeek.THURSDAY))
      )
    ),
    // Fridays
    Seq(
      // "http://urlhere.gif"
    ).map(url =>
      Greeting(
        GreetingContent.Image(url),
        GreetingType.CaliEvening(Set(DayOfWeek.FRIDAY))
      )
    ),
    // Saturdays
    Seq(
      // "http://urlhere.gif"
    ).map(url =>
      Greeting(
        GreetingContent.Image(url),
        GreetingType.CaliEvening(Set(DayOfWeek.SATURDAY))
      )
    )
  ).flatten
}
