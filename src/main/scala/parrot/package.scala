import ackcord.{CacheSnapshot, DiscordClient}
import ackcord.data.{OutgoingEmbed, OutgoingEmbedImage, TextChannelId}
import ackcord.requests.{CreateMessage, CreateMessageData}
import parrot.settings.Settings

package object parrot {
  implicit class RichDiscordClient(private val client: DiscordClient)
      extends AnyVal {

    def sendTextToActive(text: String)(implicit c: CacheSnapshot): Unit = {
      client.requestsHelper.run(
        CreateMessage(
          TextChannelId(Settings.textChannelId),
          CreateMessageData(content = text)
        )
      )
    }

    def sendImageToActive(url: String)(implicit c: CacheSnapshot): Unit = {
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
