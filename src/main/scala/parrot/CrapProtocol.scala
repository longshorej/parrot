package parrot

/** An ad-hoc line-based protocol that acks in reactions */
object CrapProtocol {
  val WordleNew: String = "!wordle new"
  val WordleGuessPrefix: String = "!wordle"
  val WordleQuit: String = "!wordle quit"
  val Start: String = "!parrot-start"
  val Stop: String = "!parrot-stop"

  val StopResponse: String = "☹️"
  val StartResponse: String = "\uD83D\uDE0A"

}
