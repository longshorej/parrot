package parrot.settings

case class Greeting()

// @TODO use akka config
// @TODO use range?
object Settings {
  val apiBaseUrl: String = "https://discord.com"
  val apiVersion: Int = 9
  val botToken: String = sys.env.getOrElse("PARROT_BOT_TOKEN", "xyz")

  val reactions: ReactionsSettings = new ReactionsSettings
  val wordle: WordleSettings = new WordleSettings
}
