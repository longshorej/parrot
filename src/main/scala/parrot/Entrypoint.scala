package parrot

import ackcord.ClientSettings
import akka.actor.typed.ActorSystem

object Entrypoint {
  def main(args: Array[String]): Unit = {
    val clientSettings = ClientSettings(Settings.BotToken)

    ActorSystem(Guardian(clientSettings), "guardian")
  }
}
