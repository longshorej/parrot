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
    if (guess.length != Length || word.length != Length)
      guess.toVector.map(_ => Status.NotInWord)
    else if (guess == word)
      guess.toVector.map(_ => Status.Correct)
    else {
      val guessAndWord = guess.zip(word)

      val corrects = guessAndWord
        .flatMap { case (g, w) => if (g == w) Some(g) else None }
        .groupBy(identity)
        .mapValues(_.length)

      val budget = word
        .groupBy(identity)
        .map { case (c, e) => c -> (e.length - corrects.getOrElse(c, 0)) }

      val (_, result) = guessAndWord
        .foldLeft(budget -> Vector.empty[Status]) {
          case ((remaining, a), (g, w)) if g == w =>
            remaining -> (a :+ Status.Correct)

          case ((remaining, a), (g, _)) =>
            remaining.get(g) match {
              case Some(n) if n > 0 =>
                remaining.updated(g, n - 1) -> (a :+ Status.InWord)
              case _ =>
                remaining -> (a :+ Status.NotInWord)
            }
        }

      result
    }
}
