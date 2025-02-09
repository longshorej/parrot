import ackcord.{CacheSnapshot, DiscordClient}
import ackcord.data.{OutgoingEmbed, OutgoingEmbedImage, TextChannelId}
import ackcord.requests.{CreateMessage, CreateMessageData}
import parrot.settings.Settings

import scala.concurrent.{ExecutionContext, Future}

package object parrot {
  implicit class RichDiscordClient(private val client: DiscordClient)
      extends AnyVal {

    def sendTextToActive(
        text: String
    )(implicit c: CacheSnapshot, ec: ExecutionContext): Future[Unit] = {
      client.requestsHelper
        .run(
          CreateMessage(
            TextChannelId(Settings.textChannelId),
            CreateMessageData(content = text)
          )
        )
        .value
        .map(_ => ())
    }

    def sendImageToActive(
        url: String
    )(implicit c: CacheSnapshot, ec: ExecutionContext): Unit = {
      client.requestsHelper.run(
        CreateMessage(
          TextChannelId(Settings.textChannelId),
          CreateMessageData(
            content = "",
            embed = Some(
              OutgoingEmbed(
                url = Some(url),
                image = Some(OutgoingEmbedImage(url))
              )
            )
          )
        )
      )
    }
  }
}
