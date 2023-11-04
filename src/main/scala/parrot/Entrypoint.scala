package parrot

import ackcord.ClientSettings
import akka.actor.typed.ActorSystem
import com.typesafe.scalalogging.StrictLogging
import parrot.settings.Settings

object Entrypoint extends StrictLogging {
  def main(args: Array[String]): Unit = {
    val clientSettings = ClientSettings(Settings.botToken)

    ActorSystem(Guardian(clientSettings), "guardian")
  }
}
