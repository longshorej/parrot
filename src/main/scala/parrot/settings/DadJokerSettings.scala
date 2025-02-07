package parrot.settings

import parrot.dadjokes.DadJoke

import java.nio.file.{Files, Paths}
import scala.concurrent.duration._

class DadJokerSettings {
  private val botJokesPath: String =
    sys.env.getOrElse("PARROT_DAD_JOKES_PATH", "")

  val tickInterval: FiniteDuration = 1.second

  val jokes: Seq[DadJoke] = {
    // crude and uses a ton of memory, but w/e who gives a shit
    // @TODO use a library
    var out = Vector.empty[DadJoke]

    if (botJokesPath.nonEmpty) {
      val data = Files.readString(Paths.get(botJokesPath))

      for {
        line <- data.linesIterator.drop(1) // drop headers
        if !line.contains('\\') && !line.contains("\t") && !line.contains(
          "*"
        ) && !line.contains("/") && !line.contains("imgur") && !line.contains(
          ":"
        )
        parts = line.split('"')
        if parts.length == 4
        call = parts(1).trim.replaceAll(" +", " ")
        response = parts(3).trim
        if call.nonEmpty && response.nonEmpty
      } {
        out :+= DadJoke(out.length, call, response)
      }
    }

    out
  }
}
