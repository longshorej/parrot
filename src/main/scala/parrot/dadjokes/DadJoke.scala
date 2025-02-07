package parrot.dadjokes

/** A dad joke. Example:
  *
  * Call = What did one wall say to the other?
  * Response = I'll meet you at the corner!
  */
case class DadJoke(id: Int, call: String, response: String)
