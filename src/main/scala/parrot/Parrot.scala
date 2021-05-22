package parrot

import ackcord._
import ackcord.data._
import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage, WebSocketRequest}
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.SourceQueueWithComplete
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import spray.json._

import scala.util.{Failure, Success, Try}

/** The meat -- accepts messages that are plain text
  *  only, and responds with a collection of reactions.
  */
object getReactions {
  type DiscordReaction = String // unsure how it's modeled

  def apply(message: String): List[String] =
    if (message.forall(isStrictlyAlphaNumeric))
      process(message)
    else
      List.empty

  // char has methods for checking things like isDigit
  // but they include some unicode characters
  private def isStrictlyAlphaNumeric(char: Char): Boolean =
    (char >= 'a' && char <= 'z') || (char >= 'A' && char <= 'Z') || (char >= '0' && char <= '9')

  private def process(message: String): List[String] =
    if (message.length > Settings.TermMax || message.length < Settings.TermMin)
      List.empty
    else
      message
        .foldLeft(Settings.CharMappings -> List.empty[String]) {
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
}

object parseGetApiGatewayBotResponse {
  import spray.json._
// parrot
  object JsonProto extends DefaultJsonProtocol {
    implicit val sessionStartLimit: JsonFormat[SessionStartLimit] =
      jsonFormat(SessionStartLimit.apply, "total", "remaining", "reset_after", "max_concurrency")
    implicit val resultFormat: RootJsonFormat[Result] = jsonFormat(Result.apply, "url", "shards", "session_start_limit")
  }

  import JsonProto._

  case class SessionStartLimit(total: Int, remaining: Int, resetAfter: Int, maxConcurrency: Int)
  case class Result(url: String, shards: Int, sessionStartLimit: SessionStartLimit)

  def apply(response: String): Either[Throwable, Result] =
    Try(response.parseJson.convertTo[Result]).toEither
}

// @TODO use akka config
// @TODO use range?
object Settings {
  val ApiBaseUrl: String = "https://discord.com"
  val ApiVersion: Int = 9
  val BotToken: String = sys.env.getOrElse("PARROT_BOT_TOKEN", "xyz")
  val CharMappings: Map[Char, List[String]] = Map(
    'l' -> List("l", "1"),
    'o' -> List("o")
  )

  val TermMin: Int = 1
  val TermMax: Int = 9
}

object Entrypoint {
  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem = ActorSystem()
    implicit val ec: ExecutionContext = system.dispatcher


    AkkaHttpDiscordApi.create().onComplete {
      case Failure(exception) =>
        // @TODO shutdown, exit nonzero

      case Success(client) =>
        client.streamEvents(UUID.randomUUID()).map { _ =>

        }
    }
  }
}

object DiscordApi {
  type Reaction = Unit // todo
}

trait DiscordApi {
  def reactToPost(postId: UUID, reaction: DiscordApi.Reaction): Future[Unit]

  def streamEvents(channelId: UUID): Future[Unit]
}

object AkkaHttpDiscordApi {
  def create()(implicit system: ActorSystem): Future[AkkaHttpDiscordApi] = {
    implicit val ec: ExecutionContext = system.dispatcher

    val clientSettings = ClientSettings(Settings.BotToken)

    clientSettings
      .createClient()
      .map(new AkkaHttpDiscordApi(_))
  }
}

class AkkaHttpDiscordApi(client: DiscordClient)(implicit system: ActorSystem) {
  private implicit val ec: ExecutionContext = system.dispatcher
  private val discordApiTimeout = 10.seconds


  //The client settings contains an excecution context that you can use before you have access to the client
  //import clientSettings.executionContext

  //The client also contains an execution context
  //import client.executionContext

  client.onEventSideEffectsIgnore {
    case APIMessage.Ready(_) => println("Now ready")
  }







  private val http = Http()

  private val authorizationHeader = RawHeader(
    "Authorization",
    s"Bot ${Settings.BotToken}"
  )

  private val baseUri = Uri(Settings.ApiBaseUrl)

  def reactToPost(postId: UUID, reaction: DiscordApi.Reaction): Future[Unit] = {
    Future.successful(())
  }

  def streamEvents(channelId: UUID): Future[Unit] = {
    strictRequest(HttpRequest(
      HttpMethods.GET,
      uriFor(Uri.Path("/api/gateway/bot")),
      List(authorizationHeader)
    )).map {
      case (response, entity) if response.status.isSuccess && response.entity.contentType == ContentTypes.`application/json` =>
        parseGetApiGatewayBotResponse(entity.data.utf8String) match {
          case Left(throwable) =>
          case Right(value) =>
            println(value)

            case class State()

            val flow = Flow[Message]
              .scan(State() -> Option.empty[Message]) {
                case ((state, _), TextMessage.Streamed(source)) =>
                  system.log.warning("dropping a TextMessage.Streamed")

                  source.runWith(Sink.ignore)

                  state -> None

                case ((state, _), TextMessage.Strict(data)) =>
                  system.log.info("received TextMessage.Strict({})", data)

                  state -> None

                case ((state, _), BinaryMessage.Streamed(source)) =>
                  system.log.warning("dropping a TextMessage.Streamed")

                  source.runWith(Sink.ignore)

                  state -> None

                case ((state, _), BinaryMessage.Strict(_)) =>
                  system.log.warning("dropping a BinaryMessage.Strict")

                  state -> None
              }
              .collect {
                case (_, Some(out)) => out
              }


            // upgradeResponse is a Future[WebSocketUpgradeResponse] that
            // completes or fails when the connection succeeds or fails
            // and closed is a Future[Done] representing the stream completion from above
            val (upgradeResponse, closed) =
            Http().singleWebSocketRequest(WebSocketRequest(value.url), flow)

            val connected = upgradeResponse.map { upgrade =>
              // just like a regular http request we can access response status which is available via upgrade.response.status
              // status code 101 (Switching Protocols) indicates that server support WebSockets
              if (upgrade.response.status == StatusCodes.SwitchingProtocols) {
                Done
              } else {
                throw new RuntimeException(s"Connection failed: ${upgrade.response.status}")
              }
            }

            // in a real application you would not side effect here
            // and handle errors more carefully
            connected.onComplete(println)








        }
      println(entity)
      println(response.status)

      ()

      case other =>
        ???
    }
  }

  private def uriFor(path: Uri.Path): Uri =
    Uri(Settings.ApiBaseUrl).withPath(path)

  private def strictRequest(request: HttpRequest): Future[(HttpResponse, HttpEntity.Strict)] =
    for {
      response <- http.singleRequest(request)
      entity   <- response.entity.toStrict(discordApiTimeout)
    } yield response -> entity
}
