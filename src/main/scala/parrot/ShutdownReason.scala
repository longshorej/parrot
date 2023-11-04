package parrot

import akka.actor.CoordinatedShutdown

sealed trait ShutdownReason extends CoordinatedShutdown.Reason

object ShutdownReason {
  case object ServiceFailure extends ShutdownReason
}
