package parrot.impls

import com.typesafe.scalalogging.StrictLogging

import java.time.{DayOfWeek, Instant, LocalDate, LocalTime, ZoneId}
import java.util.concurrent.ThreadLocalRandom
import scala.concurrent.duration._
import scala.util.Random

/** Companion object for [[DailyThingSelector]] */
object DailyThingSelector {

  /** Describes a thing, defining a set of days that things can be selected on (empty means all), and
    * a string representation.
    */
  case class ThingDescriptor(days: Set[DayOfWeek], show: String)
}

/** [[DailyThingSelector]] randomly selects some things once per day during a specified date
  * range. It is expected to be driven by ticks that fire at regular intervals. It
  * guarantees the same things will not be selected twice in a row, unless there are not enough
  * things available.
  */
class DailyThingSelector[A](
    name: String,
    things: Seq[A],
    startHour: Int,
    rangeHours: Int,
    selectionSize: Int
)(
    thingDescriptor: A => DailyThingSelector.ThingDescriptor
) extends StrictLogging {
  import DailyThingSelector._

  private val zoneId = ZoneId.of("America/Los_Angeles")
  private var nextRun = Option.empty[Instant]
  private var previousThings = Set.empty[A]

  def tick(): IndexedSeq[A] = {
    val now = Instant.now()
    val nowZoned = now.atZone(zoneId)
    val nowDate = nowZoned.toLocalDate

    def updateNextRun(allowToday: Boolean): Unit = {
      val nextTenSecondsHack = false
      val rand = ThreadLocalRandom.current()
      val time =
        if (nextTenSecondsHack)
          Instant.now().atZone(zoneId).toLocalTime.plusSeconds(10)
        else
          LocalTime
            .of(startHour, 0)
            .plusSeconds(rand.nextLong(rangeHours.hours.toSeconds))

      if (allowToday && (nextTenSecondsHack || nowZoned.getHour < startHour)) {
        val nextRunInstant = nowDate.atTime(time).atZone(zoneId).toInstant
        nextRun = Some(nextRunInstant)
        logger.info(
          s"$name: updated next run to later today, utc=$nextRunInstant pacific=${nextRunInstant.atZone(zoneId)}"
        )
      } else {
        val nextRunInstant =
          nowDate.plusDays(1).atTime(time).atZone(zoneId).toInstant
        nextRun = Some(nextRunInstant)
        logger.info(
          s"$name: updated next run to tomorrow, value=$nextRunInstant pacific=${nextRunInstant.atZone(zoneId)}"
        )
      }
    }

    // every tick:
    //
    // inspect our next scheduled run. if it's now or in the past, select things and update
    // the next run to happen tomorrow. if it is yet to come, do nothing. if it has yet to
    // exist, schedule it and allow it to happen today.

    nextRun match {
      case Some(value) if now == value || now.isAfter(value) =>
        val selected = selectThings(nowDate)
        previousThings = selected.map(_._1).toSet

        logger.info(
          s"$name: reached targeted time, now=$now nowZoned=$nowZoned nowDate=$nowDate time=$value selected=${selected
            .map(_._2.show)
            .mkString(",")}"
        )

        updateNextRun(allowToday = false)

        selected.map(_._1)

      case Some(_) =>
        IndexedSeq.empty

      case None =>
        updateNextRun(allowToday = true)

        IndexedSeq.empty
    }
  }

  private def selectThings(
      tickDate: LocalDate
  ): IndexedSeq[(A, ThingDescriptor)] = {
    val rand = ThreadLocalRandom.current()
    val dayOfWeek = tickDate.getDayOfWeek

    val ts = things.iterator
      .filterNot(previousThings.contains)
      .map(t => t -> thingDescriptor(t))
      .filter(_._2.days.contains(dayOfWeek))
      .toIndexedSeq

    if (ts.size <= selectionSize)
      new Random(rand).shuffle(ts)
    else {
      @annotation.tailrec
      def step(
          pool: IndexedSeq[(A, ThingDescriptor)],
          accum: IndexedSeq[(A, ThingDescriptor)]
      ): IndexedSeq[(A, ThingDescriptor)] =
        if (accum.size == selectionSize) {
          accum
        } else {
          require(pool.nonEmpty)

          val selectedIndex = rand.nextInt(pool.length)
          val selected = pool(selectedIndex)

          step(pool = remove(pool, selectedIndex), accum = accum :+ selected)
        }

      step(ts, IndexedSeq.empty)
    }
  }

  private def remove[B](collection: IndexedSeq[B], i: Int): IndexedSeq[B] =
    collection.take(i) ++ collection.drop(i + 1)

}
