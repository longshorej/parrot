package parrot.impls

import com.typesafe.scalalogging.StrictLogging
import parrot.settings.ScheduledGreetingsSettings.{
  Greeting,
  GreetingContent,
  GreetingType
}

import java.time.temporal.ChronoUnit
import java.time.{Instant, LocalDate, ZoneId}
import java.util.concurrent.ThreadLocalRandom

sealed abstract class GreetingTypeImpl {
  def tick(now: Long): Seq[GreetingContent]
}

object GreetingTypeImpl {
  class CaliMorningImpl(greetings: Seq[Greeting[GreetingType.CaliMorning]])
      extends GreetingTypeImpl
      with StrictLogging {
    require(greetings.nonEmpty)

    private val zoneId = ZoneId.of("America/Los_Angeles")
    private var lastTick = 0L
    private var targetTick = 0L

    override def tick(tick: Long): Seq[GreetingContent] = {
      logger.trace(s"received tick=$tick")

      val rand = ThreadLocalRandom.current()

      val tickInstant = Instant.ofEpochMilli(tick)
      val lastTickInstant = Instant.ofEpochMilli(lastTick)
      val lastTickZoned = lastTickInstant.atZone(zoneId)
      val lastTickDate = lastTickZoned.toLocalDate
      val tickZoned = tickInstant.atZone(zoneId)
      val tickDate = tickZoned.toLocalDate

      if (lastTickDate != tickDate) {
        // ticks are every 1 second, and our desired behavior is to post randomly between [8, 10] pacific
        // @TODO not sure this block is correct

        val tickZonedStartOfDay = tickZoned.truncatedTo(ChronoUnit.DAYS)
        val targetStart = tickZonedStartOfDay.plusHours(8)
        val targetStartMs = targetStart.toInstant.toEpochMilli
        val targetEnd = tickZonedStartOfDay.plusHours(10)
        val targetEndMs = targetEnd.toInstant.toEpochMilli
        val range = targetEndMs - targetStartMs
        //val targetMs = targetStartMs + rand.nextLong(range)
        val targetMs = tick + 3000L // @TODO local hack for testing
        val targetZoned = Instant.ofEpochMilli(targetMs).atZone(zoneId)
        val minutesUntilTick = (targetMs - tick) / 1000d / 60

        targetTick = targetMs

        logger.info(
          s"new date, lastTickDate=$lastTickDate tickDate=$tickDate targetTick=$targetTick targetZoned=$targetZoned minutesUntilTick=$minutesUntilTick"
        )
      }

      if (tick == targetTick) {
        val selectedGreetings = selectGreetings(tickDate)

        logger.info(
          s"targeted tick has ticked, selectedGreetings=${selectedGreetings.mkString(", ")}"
        )

        targetTick = 0
        lastTick = tick

        selectedGreetings
      } else {
        lastTick = tick

        Seq.empty
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
