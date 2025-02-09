package parrot.impls

import parrot.settings.ScheduledGreetingsSettings.{
  Greeting,
  GreetingContent,
  GreetingType
}

import java.time.Instant

sealed abstract class GreetingTypeImpl {
  def tick(now: Instant): Seq[GreetingContent]
}

object GreetingTypeImpl {
  object CaliMorningImpl {
    val StartHour: Int = 8
    val RangeHours: Int = 2
  }

  class CaliMorningImpl(greetings: Seq[Greeting[GreetingType.CaliMorning]])
      extends GreetingTypeImpl {

    import CaliMorningImpl._

    private val greetingSelector =
      new DailyThingSelector(
        "cali-morning",
        greetings,
        StartHour,
        RangeHours,
        1
      )(g =>
        DailyThingSelector.ThingDescriptor(g.greetingType.days, g.toString)
      )

    override def tick(now: Instant): Seq[GreetingContent] = {
      greetingSelector.tick(now).map(_.content).toList
    }
  }

  object CaliEveningImpl {
    val StartHour: Int = 22
    val RangeHours: Int = 2
  }

  class CaliEveningImpl(greetings: Seq[Greeting[GreetingType.CaliEvening]])
      extends GreetingTypeImpl {

    import CaliEveningImpl._

    private val greetingSelector =
      new DailyThingSelector(
        "cali-evening",
        greetings,
        StartHour,
        RangeHours,
        1
      )(g =>
        DailyThingSelector.ThingDescriptor(g.greetingType.days, g.toString)
      )

    override def tick(now: Instant): Seq[GreetingContent] =
      greetingSelector.tick(now).map(_.content).toList
  }
}
