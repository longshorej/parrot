package parrot

// @TODO use akka config
// @TODO use range?
object Settings {
  val ApiBaseUrl: String = "https://discord.com"
  val ApiVersion: Int = 9
  val BotToken: String = sys.env.getOrElse("PARROT_BOT_TOKEN", "xyz")
  val LeetCharMappings: Map[Char, Char] =
    Map('l' -> '1', 'o' -> '0', 's' -> '5', 'e' -> '3')
  val CharMappings: Map[Char, List[String]] = Map(
    'a' -> List("\uD83C\uDDE6"),
    'b' -> List("\uD83C\uDDE7"),
    'c' -> List("\uD83C\uDDE8"),
    'd' -> List("\uD83C\uDDE9"),
    'e' -> List("\uD83C\uDDEA"),
    'f' -> List("\uD83C\uDDEB"),
    'g' -> List("\uD83C\uDDEC"),
    'h' -> List("\uD83C\uDDED"),
    'i' -> List("\uD83C\uDDEE"),
    'j' -> List("\uD83C\uDDEF"),
    'k' -> List("\uD83C\uDDF0"),
    'l' -> List("\uD83C\uDDF1"),
    'm' -> List("\uD83C\uDDF2"),
    'n' -> List("\uD83C\uDDF3"),
    'o' -> List("\uD83C\uDDF4"),
    'p' -> List("\uD83C\uDDF5"),
    'q' -> List("\uD83C\uDDF6"),
    'r' -> List("\uD83C\uDDF7"),
    's' -> List("\uD83C\uDDF8"),
    't' -> List("\uD83C\uDDF9"),
    'u' -> List("\uD83C\uDDFA"),
    'v' -> List("\uD83C\uDDFB"),
    'w' -> List("\uD83C\uDDFC"),
    'x' -> List("\uD83C\uDDFD"),
    'y' -> List("\uD83C\uDDFE"),
    'z' -> List("\uD83C\uDDFF"),
    '0' -> List("0️⃣"),
    '1' -> List("1️⃣"),
    '2' -> List("2️⃣"),
    '3' -> List("3️⃣"),
    '4' -> List("4️⃣"),
    '5' -> List("5️⃣"),
    '6' -> List("6️⃣"),
    '7' -> List("7️⃣"),
    '8' -> List("8️⃣"),
    '9' -> List("9️⃣"),
    '$' -> List("\uD83D\uDCB2")
  )
  val TermMin: Int = 1
  val TermMax: Int = 4
}
