package parrot.logic

import parrot.Settings

object getReactions {
  private val preparedMappings =
    Settings.CharMappings ++ Settings.LeetCharMappings.map {
      case (key, value) =>
        key -> (Settings.CharMappings.getOrElse(
          key,
          Nil
        ) ++ Settings.CharMappings.getOrElse(value, Nil))
    }

  def apply(message: String): List[String] =
    if (message.forall(isStrictlyAlphaNumericOrLimitedSymbolic))
      processPrimary(message)
        .orElse(processConsecutive(message))
        .getOrElse(Nil)
    else
      List.empty

  // char has methods for checking things like isDigit
  // but they include some unicode characters
  private def isStrictlyAlphaNumericOrLimitedSymbolic(char: Char): Boolean =
    (char >= 'a' && char <= 'z') || (char >= 'A' && char <= 'Z') || (char >= '0' && char <= '9') || char == '$'

  /** Primary logic that converts based on entries in a lookup table. */
  private def processPrimary(message: String): Option[List[String]] =
    if (message.length > Settings.TermMax || message.length < Settings.TermMin)
      None
    else {
      val result = message.toLowerCase
        .foldLeft(preparedMappings -> List.empty[String]) {
          case (state @ (mappings, entries), char) =>
            mappings.get(char) match {
              case Some(head :: tail) =>
                mappings.updated(char, tail) -> (head :: entries)

              case Some(_) =>
                state

              case None =>
                state
            }
        }
        ._2
        .reverse

      if (result.length == message.length)
        Some(result)
      else
        None
    }

  /** Processes the message by collapsing consecutive characters. e.g. "aaabbbaaa" becomes "aabbaa" which
    * is then reacted to with the primary method. A character must occur at least 3 times to be collapsed,
    * and will first be collapsed into a string of 2 chars to retain some of the original style, and allow
    * proper spelling of most words, since in english two consecutive chars may occur in a valid word, but
    * rarely 3 (if ever?). If no good with 2, we try with 1.
    */
  private def processConsecutive(message: String): Option[List[String]] = {
    val min = 3

    def collapse(max: Int): String = {
      val (reduced, _) = message.foldLeft("" -> Option.empty[Char]) {
        case (current @ (_, d), c) if d.contains(c) =>
          current

        case ((a, _), c) if a.takeRight(min - 1).count(_ == c) == min =>
          (a.dropRight(max - 1) + c) -> Some(c)

        case ((a, _), c) if a.takeRight(max).count(_ == c) == max =>
          a -> Some(c)

        case ((a, _), c) =>
          (a + c) -> None
      }

      reduced
    }

    processPrimary(collapse(max = 2))
      .orElse(processPrimary(collapse(max = 1)))
  }
}
