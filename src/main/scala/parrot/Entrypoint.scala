package parrot

import ackcord.ClientSettings
import akka.actor.typed.ActorSystem
import parrot.settings.Settings

object Entrypoint {
  def main(args: Array[String]): Unit = {
    val clientSettings = ClientSettings(Settings.botToken)

    ActorSystem(Guardian(clientSettings), "guardian")
  }
}
