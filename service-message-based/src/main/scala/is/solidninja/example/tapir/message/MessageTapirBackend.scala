package is.solidninja.example.tapir.message

import sttp.capabilities
import sttp.model.{HasHeaders, Header, Method, QueryParams, Uri}
import sttp.tapir.capabilities.NoStreams
import sttp.tapir.model.{ConnectionInfo, ServerRequest}
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.interceptor.RequestResult
import sttp.tapir.server.interpreter._
import sttp.tapir.{AttributeKey, AttributeMap, CodecFormat, RawBodyType, WebSocketBodyOutput}
import zio._

import java.io.{ByteArrayInputStream, InputStream}
import java.nio.ByteBuffer
import java.nio.charset.Charset
import scala.util.{Success, Try}

private[message] object Unsupported {
  val Streams = new UnsupportedOperationException("streaming not supported")
  val Files = new UnsupportedOperationException("files not supported")
  val Multipart = new UnsupportedOperationException("multipart not supported")
}

/** @note
  *   converter from [[Message]] --> [[ServerRequest]]
  */
case class MessageServerRequest(message: Message, attributes: AttributeMap = AttributeMap.Empty) extends ServerRequest {
  override def protocol: String = "HTTP/1.1"
  override def connectionInfo: ConnectionInfo = ConnectionInfo(None, None, None)
  override def underlying: Any = message
  override def pathSegments: List[String] = message.uri.pathSegments.segments.map(_.v).toList
  override def queryParameters: QueryParams = message.uri.params
  override def attribute[T](k: AttributeKey[T]): Option[T] = attributes.get(k)
  override def attribute[T](k: AttributeKey[T], v: T): ServerRequest = copy(attributes = attributes.put(k, v))
  override def withUnderlying(underlying: Any): ServerRequest =
    MessageServerRequest(underlying.asInstanceOf[Message], attributes)
  override def method: Method = message.method
  override def uri: Uri = message.uri
  override def headers: Seq[Header] = message.headers.map((Header.apply _).tupled).toSeq
}

class MessageResponseListener[R, E] extends BodyListener[ZIO[R, E, *], MessageResponse] {
  override def onComplete(body: MessageResponse)(cb: Try[Unit] => ZIO[R, E, Unit]): ZIO[R, E, MessageResponse] =
    cb(Success(())).as(body)
}

class MessageRequestBody[R, E]() extends RequestBody[ZIO[R, E, *], NoStreams] {
  override val streams: capabilities.Streams[NoStreams] = NoStreams
  override def toStream(serverRequest: ServerRequest): streams.BinaryStream = throw new NotImplementedError(
    "no streaming support"
  )
  override def toRaw[Req](serverRequest: ServerRequest, bodyType: RawBodyType[Req]): ZIO[R, E, RawValue[Req]] = {
    val underlying = serverRequest.underlying.asInstanceOf[Message]
    bodyType match {
      case RawBodyType.StringBody(charset) => ZIO.succeed(new String(underlying.body, charset)).map(RawValue(_))
      case RawBodyType.ByteArrayBody       => ZIO.succeed(underlying.body).map(RawValue(_))
      case RawBodyType.ByteBufferBody      => ZIO.succeed(ByteBuffer.wrap(underlying.body)).map(RawValue(_))
      case RawBodyType.InputStreamBody     => ZIO.succeed(new ByteArrayInputStream(underlying.body)).map(RawValue(_))
      case RawBodyType.FileBody            => ZIO.die(Unsupported.Files)
      case _: RawBodyType.MultipartBody    => ZIO.die(Unsupported.Multipart)
    }
  }
}

class MessageResponseBody() extends ToResponseBody[MessageResponse, NoStreams] {
  override def fromRawValue[Resp](
      v: Resp,
      headers: HasHeaders,
      format: CodecFormat,
      bodyType: RawBodyType[Resp]
  ): MessageResponse = {
    val arr = bodyType match {
      case RawBodyType.StringBody(charset) => v.asInstanceOf[String].getBytes(charset)
      case RawBodyType.ByteArrayBody       => v.asInstanceOf[Array[Byte]]
      case RawBodyType.ByteBufferBody      => v.asInstanceOf[ByteBuffer].array()
      case RawBodyType.InputStreamBody     => v.asInstanceOf[InputStream].readAllBytes()
      case RawBodyType.FileBody            => throw Unsupported.Files
      case _: RawBodyType.MultipartBody    => throw Unsupported.Multipart
    }

    MessageResponse(Message(mkHeaders(headers), arr))
  }

  private def mkHeaders(headers: HasHeaders): Map[String, String] = headers.headers.map(h => h.name -> h.value).toMap

  override val streams: capabilities.Streams[NoStreams] = NoStreams
  override def fromStreamValue(
      v: streams.BinaryStream,
      headers: HasHeaders,
      format: CodecFormat,
      charset: Option[Charset]
  ): MessageResponse = throw Unsupported.Streams
  override def fromWebSocketPipe[REQ, RESP](
      pipe: streams.Pipe[REQ, RESP],
      o: WebSocketBodyOutput[streams.Pipe[REQ, RESP], REQ, RESP, _, NoStreams]
  ): MessageResponse = throw Unsupported.Streams
}

final case class MessageTapirBackend[R, E](
    endpoints: List[ServerEndpoint[Any, ZIO[R, E, *]]],
    convertError: Throwable => IO[E, Nothing]
) {

  private[this] implicit val monad: ZIOMonadAsyncError[R, E] = new ZIOMonadAsyncError(convertError)
  private[this] implicit val bodyListener: BodyListener[ZIO[R, E, *], MessageResponse] = new MessageResponseListener

  private val interp = new ServerInterpreter[Any, ZIO[R, E, *], MessageResponse, NoStreams](
    serverEndpoints = FilterServerEndpoints(endpoints),
    requestBody = new MessageRequestBody,
    toResponseBody = new MessageResponseBody,
    interceptors = Nil,
    deleteFile = _ => ZIO.die(Unsupported.Files)
  )

  def handle(in: Message): ZIO[R, E, RequestResult[MessageResponse]] =
    interp.apply(MessageServerRequest(in))

}
