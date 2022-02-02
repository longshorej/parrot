import parrot._

import org.scalatest.flatspec._
import org.scalatest.matchers._

class ParrotTests extends AnyFlatSpec with should.Matchers {
  "getReactions" should "work" in {
    logic.getReactions("lol") shouldBe List("\uD83C\uDDF1", "\uD83C\uDDF4", "1️⃣")
  }
}
