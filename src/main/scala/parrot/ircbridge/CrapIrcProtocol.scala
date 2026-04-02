package parrot.ircbridge

object CrapIrcProtocol {
  sealed trait Incoming

  object Incoming {
    final case class Ping(token1: String, token2: Option[String])
        extends Incoming

    final case class Welcome001(
        server: Option[String],
        nick: String,
        text: String
    ) extends Incoming

    final case class ChannelPrivmsg(
        channel: String,
        text: String,
        nick: Option[String]
    ) extends Incoming

    def parse(line: String): Option[Incoming] = {
      Some(line.stripSuffix("\r").stripSuffix("\n").trim)
        .filter(_.nonEmpty)
        .flatMap { s =>
          val (head, maybeTrailing) = {
            val idx = s.indexOf(" :")
            if (idx >= 0) (s.substring(0, idx), Some(s.substring(idx + 2)))
            else (s, None)
          }

          val headParts = head.split(" ").toList.map(_.trim).filter(_.nonEmpty)

          val (maybePrefix, parts) =
            headParts match {
              case p +: rest if p.startsWith(":") =>
                (Some(p.drop(1).takeWhile(_ != '!')), rest)
              case other =>
                (None, other)
            }

          parts match {
            case cmd +: rest if cmd.equalsIgnoreCase("PING") =>
              maybeTrailing match {
                case Some(tok) => Some(Ping(tok, None))
                case None =>
                  rest match {
                    case t1 +: t2 +: _ => Some(Ping(t1, Some(t2)))
                    case t1 +: Nil     => Some(Ping(t1, None))
                    case _             => None
                  }
              }

            case "001" +: nick +: _ =>
              maybeTrailing.map(txt => Welcome001(maybePrefix, nick, txt))

            case cmd +: target +: _ if cmd.equalsIgnoreCase("PRIVMSG") =>
              Option
                .when(target.startsWith("#") || target.startsWith("&"))(
                  maybeTrailing
                    .map(txt => ChannelPrivmsg(target, txt, maybePrefix))
                )
                .flatten

            case _ =>
              None
          }
        }

    }
  }

  sealed trait Outgoing {
    def render: String
  }

  object Outgoing {
    final case class Pong(token1: String, token2: Option[String])
        extends Outgoing {
      override def render: String =
        token2 match {
          case Some(token2) => s"PONG $token1 $token2"
          case None         => s"PONG :$token1"
        }
    }

    final case class Mode(target: String, flags: String) extends Outgoing {
      override def render: String = s"MODE $target $flags"
    }

    final case class Nick(nick: String) extends Outgoing {
      override def render: String = s"NICK $nick"
    }

    final case class User(
        username: String,
        mode: String,
        unused: String,
        realname: String
    ) extends Outgoing {
      override def render: String = s"USER $username $mode $unused :$realname"
    }

    final case class Join(channel: String) extends Outgoing {
      override def render: String = s"JOIN $channel"
    }

    final case class Privmsg(channel: String, text: String) extends Outgoing {
      override def render: String = s"PRIVMSG $channel :$text"
    }
  }
}
