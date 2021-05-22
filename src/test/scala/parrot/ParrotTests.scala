import parrot._

import org.scalatest.flatspec._
import org.scalatest.matchers._

class ParrotTests extends AnyFlatSpec with should.Matchers {
  "getReactions" should "work" in {
    getReactions("lol") shouldBe List("l", "o", "1")
  }

  "parseGetApiGatewayBotResponse" should "work" in {
    parseGetApiGatewayBotResponse(
      """{"url": "wss://gateway.discord.gg", "shards": 1, "session_start_limit": {"total": 1000, "remaining": 1000, "reset_after": 0, "max_concurrency": 1}}"""
    ) shouldBe Right(
      parseGetApiGatewayBotResponse.Result(
        url = "wss://gateway.discord.gg",
        shards = 1,
        sessionStartLimit = parseGetApiGatewayBotResponse.SessionStartLimit(
          total = 1000,
          remaining = 1000,
          resetAfter = 0,
          maxConcurrency = 1
        )
      )
    )
  }
}
