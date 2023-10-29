package parrot.settings

object ScheduledGreetingsSettings {
  sealed trait GreetingType

  /** Each greeting has an associated type, currently this is basically a scheduling profile */
  object GreetingType {
    /** Randomly between 8 and 10am pacific */
    case object CaliMorning extends GreetingType
  }

  case class Greeting(imageSrc: String, weight: Int, greetingType: GreetingType)
}

class ScheduledGreetingsSettings {
  import ScheduledGreetingsSettings._

  val greetings: Seq[Greeting] = Seq(
    Greeting("https://media.tenor.com/_l9zt0EOc-sAAAAC/girls.gif", 100, GreetingType.CaliMorning)
  )
}
