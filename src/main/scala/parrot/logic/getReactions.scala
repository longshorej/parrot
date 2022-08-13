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

  /** Processes the message by collapsing consecutive characters. e.g. "aaabbbaaa" becomes "aba" which
    * is then reacted to with the primary method.
    */
  private def processConsecutive(message: String): Option[List[String]] =
    processPrimary(message.foldLeft("") {
      case (a, c) => if (a.endsWith(c.toString)) a else a + c
    })
}
