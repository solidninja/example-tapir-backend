package is.solidninja.example.tapir.message

import sttp.model.{Method, Uri}

/** abstract idea of a message (e.g. from a message broker)
  */
case class Message(headers: Map[String, String], body: Array[Byte]) {
  lazy val method = Method.unsafeApply(headers("X-Request-Method"))
  lazy val uri = Uri.unsafeParse(headers("X-Request-URL"))
}

case class MessageResponse(inner: Message)
