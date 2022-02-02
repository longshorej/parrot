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
      process(message)
    else
      List.empty

  // char has methods for checking things like isDigit
  // but they include some unicode characters
  private def isStrictlyAlphaNumericOrLimitedSymbolic(char: Char): Boolean =
    (char >= 'a' && char <= 'z') || (char >= 'A' && char <= 'Z') || (char >= '0' && char <= '9') || char == '$'

  private def process(message: String): List[String] =
    if (message.length > Settings.TermMax || message.length < Settings.TermMin)
      List.empty
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
        result
      else
        Nil
    }
}
