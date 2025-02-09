package parrot.settings

/** Settings, currently global for the project. See issue #3 for improvements here. */
object Settings {
  val apiBaseUrl: String = "https://discord.com"
  val apiVersion: Int = 9
  val botToken: String = sys.env.getOrElse("PARROT_BOT_TOKEN", "xyz")

  val dadJokerSettings: DadJokerSettings = new DadJokerSettings
  val reactions: ReactionsSettings = new ReactionsSettings
  val scheduledGreetings: ScheduledGreetingsSettings =
    new ScheduledGreetingsSettings

  val textChannelId: Long = {
    val Fa = 826348192084787231L
    val Personal = 845432753414602796L

    Fa
  }

  val wordle: WordleSettings = new WordleSettings
}
