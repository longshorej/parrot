import parrot._

import org.scalatest.flatspec._
import org.scalatest.matchers._

class ParrotTests extends AnyFlatSpec with should.Matchers {
  "getReactions" should "work" in {
    logic
      .getReactions("lol") shouldBe List("\uD83C\uDDF1", "\uD83C\uDDF4", "1️⃣")

    logic
      .getReactions("222222222222222222222222222222222222222222") shouldBe List(
      "2️⃣"
    )

    logic
      .getReactions("22222221111") shouldBe List("2️⃣", "1️⃣")
  }

  "evaluateWordle" should "work" in {
    logic.evaluateWordle("enter", "pleat") shouldBe Vector(
      logic.evaluateWordle.Status.InWord,
      logic.evaluateWordle.Status.NotInWord,
      logic.evaluateWordle.Status.InWord,
      logic.evaluateWordle.Status.NotInWord,
      logic.evaluateWordle.Status.NotInWord
    )

    logic.evaluateWordle("taint", "haunt") shouldBe Vector(
      logic.evaluateWordle.Status.NotInWord,
      logic.evaluateWordle.Status.Correct,
      logic.evaluateWordle.Status.NotInWord,
      logic.evaluateWordle.Status.Correct,
      logic.evaluateWordle.Status.Correct
    )
  }
}
