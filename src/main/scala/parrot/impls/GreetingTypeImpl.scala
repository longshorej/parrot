package parrot.impls

import com.typesafe.scalalogging.StrictLogging
import parrot.settings.ScheduledGreetingsSettings.{
  Greeting,
  GreetingContent,
  GreetingType
}

import java.time.{Instant, LocalDate, LocalTime, ZoneId}
import java.util.concurrent.ThreadLocalRandom

import scala.concurrent.duration._

sealed abstract class GreetingTypeImpl {
  def tick(): Seq[GreetingContent]
}

object GreetingTypeImpl {
  object CaliMorningImpl {
    val StartHour: Int = 8
    val RangeHours: Int = 2
  }

  class CaliMorningImpl(greetings: Seq[Greeting[GreetingType.CaliMorning]])
      extends GreetingTypeImpl
      with StrictLogging {
    import CaliMorningImpl._

    require(greetings.nonEmpty)

    private val zoneId = ZoneId.of("America/Los_Angeles")
    private var nextRun = Option.empty[Instant]

    override def tick(): Seq[GreetingContent] = {
      val now = Instant.now()
      val nowZoned = now.atZone(zoneId)
      val nowDate = nowZoned.toLocalDate

      def updateNextRun(allowToday: Boolean): Unit = {
        val nextMinuteHack = false
        val rand = ThreadLocalRandom.current()
        val time =
          if (nextMinuteHack)
            Instant.now().atZone(zoneId).toLocalTime.plusMinutes(1)
          else
            LocalTime
              .of(StartHour, 0)
              .plusSeconds(rand.nextLong(RangeHours.hours.toSeconds))

        if (allowToday && (nextMinuteHack || nowZoned.getHour < StartHour)) {
          val nextRunInstant = nowDate.atTime(time).atZone(zoneId).toInstant
          nextRun = Some(nextRunInstant)
          logger.info(
            s"updated next run to later today, utc=$nextRunInstant pacific=${nextRunInstant.atZone(zoneId)}"
          )
        } else {
          val nextRunInstant =
            nowDate.plusDays(1).atTime(time).atZone(zoneId).toInstant
          nextRun = Some(nextRunInstant)
          logger.info(
            s"updated next run to tomorrow, value=$nextRunInstant pacific=${nextRunInstant.atZone(zoneId)}"
          )
        }
      }

      nextRun match {
        case Some(value) if now == value || now.isAfter(value) =>
          val selectedGreetings = selectGreetings(nowDate)

          logger.info(
            s"reached targeted time, now=$now nowZoned=$nowZoned nowDate=$nowDate time=$value selectedGreetings=${selectedGreetings
              .mkString(", ")}"
          )

          updateNextRun(allowToday = false)

          selectedGreetings

        case Some(_) =>
          Nil

        case None =>
          updateNextRun(allowToday = true)

          Nil
      }
    }

    private def selectGreetings(tickDate: LocalDate): Seq[GreetingContent] = {
      val rand = ThreadLocalRandom.current()
      val gs = greetings.filter(g =>
        g.greetingType.days.contains(tickDate.getDayOfWeek)
      )
      val g = gs.lift(rand.nextInt(gs.length))
      g.map(_.content).toList
    }
  }
}
