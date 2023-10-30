import ackcord.{CacheSnapshot, DiscordClient}
import ackcord.data.{OutgoingEmbed, OutgoingEmbedImage, TextChannelId}
import ackcord.requests.{CreateMessage, CreateMessageData}

package object parrot {
  object RichDiscordClient {
    private val Fa = 826348192084787231L
    private val Personal = 845432753414602796L
    private val Active = Personal
  }
  implicit class RichDiscordClient(private val client: DiscordClient)
      extends AnyVal {

    def sendTextToActive(text: String)(implicit c: CacheSnapshot): Unit = {
      client.requestsHelper.run(
        CreateMessage(
          TextChannelId(RichDiscordClient.Active),
          CreateMessageData(text)
        )
      )
    }

    def sendImageToActive(url: String)(implicit c: CacheSnapshot): Unit = {
      client.requestsHelper.run(
        CreateMessage(
          TextChannelId(RichDiscordClient.Active),
          CreateMessageData(
            "",
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
