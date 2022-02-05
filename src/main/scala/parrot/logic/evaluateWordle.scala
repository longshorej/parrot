package parrot.logic

object evaluateWordle {
  val GuessLimit: Int = 6
  val Length: Int = 5

  sealed trait Status

  object Status {
    case object Correct extends Status
    case object InWord extends Status
    case object NotInWord extends Status
  }

  // @TODO scalafmt messes this up - tweak settings
  def apply(guess: String, word: String): Vector[Status] =
    // this algo is crude, but there's no need to generalize really, just keep it simple
    // also keep in mind that inputs are 5 in length before any y'all want to use sets
    if (guess.length != Length || word.length != Length)
      guess.toVector.map(_ => Status.NotInWord)
    else if (guess == word)
      guess.toVector.map(_ => Status.Correct)
    else {
      val initialRemaining = word
        .groupBy(identity)
        .mapValues(_.length)

      val indexedGuess = guess.toVector.zipWithIndex

      val (_, result) =
        indexedGuess.foldLeft(initialRemaining -> Vector.empty[Status]) {
          case ((remaining, a), (c, i)) if word(i) == c =>
            remaining -> (a :+ Status.Correct)

          case ((remaining, a), (c, _))
              if remaining.get(c).fold(false)(_ > 0) =>
            remaining
              .get(c)
              .fold(remaining - c)(n =>
                remaining.updated(c, n - 1)
              ) -> (a :+ Status.InWord)

          case ((remaining, a), _) =>
            remaining -> (a :+ Status.NotInWord)
        }

      result
    }
}
