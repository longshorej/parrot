package parrot

import ackcord.ClientSettings
import akka.actor.typed.ActorSystem
import com.typesafe.scalalogging.StrictLogging
import parrot.settings.Settings

object Entrypoint extends StrictLogging {
  def main(args: Array[String]): Unit = {
    val clientSettings = ClientSettings(Settings.botToken)

    logger.info(s"loaded ${Settings.dadJokerSettings.jokes.length} dad jokes")
    logger.info(
      s"loaded ${Settings.scheduledGreetings.morningGreetings.length} morning greetings"
    )
    logger.info(
      s"loaded ${Settings.scheduledGreetings.eveningGreetings.length} evening greetings"
    )

    ActorSystem(Guardian(clientSettings), "guardian")
  }
}
