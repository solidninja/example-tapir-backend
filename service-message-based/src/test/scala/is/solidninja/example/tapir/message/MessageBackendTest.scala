package is.solidninja.example.tapir.message

import io.circe.generic.auto._
import io.circe.parser.parse
import io.circe.syntax._
import io.circe.{Decoder, Printer}
import is.solidninja.example.tapir._
import org.junit.runner.RunWith
import sttp.model.StatusCode
import sttp.tapir.server.interceptor.RequestResult
import sttp.tapir.server.model.ServerResponse
import zio._
import zio.test.TestAspect.parallel
import zio.test._

import java.util.UUID

/** Test the message-based backend using the real endpoints + service implementation.
  */
@RunWith(classOf[zio.test.junit.ZTestJUnitRunner])
class MessageBackendTest extends ZIOSpecDefault {
  val backend = MessageTapirBackend[FooBarService.Environment](FooBarServerEndpoints.allRIO())
  override def spec = suite("foobar service endpoint")(
    test("GET /foo should list all foobars")(
      for {
        // 1. set up test data
        _ <- FooBarRepository.store(TestData.Foobar1)
        _ <- FooBarRepository.store(TestData.Foobar2)
        expected <- FooBarRepository.list()
        // 2. run the HTTP method through the backend
        resp <- backend.handle(TestData.ListMessage).flatMap(toResponse)
        // 3. get data out
        got <- parseJson[List[FooBar]](resp.body.get.inner.body)
      } yield {
        assertTrue(
          expected.size == 2,
          expected == got,
          resp.code == StatusCode.Ok,
          resp.header("Content-Type").contains("application/json")
        )
      }
    ).provide(FooBarRepository.stubLayer),
    test("GET /foo/:id should return the appropriate foo")(
      for {
        // 1. set up test data
        _ <- FooBarRepository.store(TestData.Foobar1)
        id2 <- FooBarRepository.store(TestData.Foobar2)
        expected <- FooBarRepository.get(id2).someOrFail(throw new RuntimeException(s"not found $id2"))
        // 2. run the HTTP method through the backend
        respFound <- backend.handle(TestData.get(id2)).flatMap(toResponse)
        respNotFound <- backend.handle(TestData.get(UUID.randomUUID())).flatMap(toResponse)
        // 3. get data out
        got <- parseJson[FooBar](respFound.body.get.inner.body)
      } yield {
        assertTrue(
          expected == got,
          respFound.code == StatusCode.Ok,
          respFound.header("Content-Type").contains("application/json"),
          respNotFound.code == StatusCode.BadRequest, // this is incorrect strictly speaking but suffices for the purposes of an example
          respNotFound.header("Content-Type").contains("application/json")
        )
      }
    ).provide(FooBarRepository.stubLayer),
    test("POST /foo/ad should add the foo and return the id")(
      for {
        // 1. set up test data
        id1 <- FooBarRepository.store(TestData.Foobar1)
        allBefore <- FooBarRepository.list()
        // 2. run the HTTP method through the backend
        resp <- backend.handle(TestData.add(TestData.Foobar2)).flatMap(toResponse)
        // 3. get data out
        allAfter <- FooBarRepository.list()
        id2 = UUID.fromString(new String(resp.body.get.inner.body))
      } yield {
        assertTrue(
          id2 != id1,
          allBefore.size == 1,
          allAfter.size == 2,
          allAfter == List(TestData.Foobar1, TestData.Foobar2),
          resp.code == StatusCode.Ok,
          resp.header("Content-Type").contains("text/plain; charset=UTF-8")
        )
      }
    ).provide(FooBarRepository.stubLayer)
  ) @@ parallel

  def toResponse[T](res: RequestResult[T]): Task[ServerResponse[T]] =
    res match {
      case RequestResult.Response(r) => ZIO.succeed(r)
      case RequestResult.Failure(failures) =>
        ZIO.fail(new RuntimeException(s"failures detected: ${failures.mkString(",")}"))
    }
  def parseJson[T: Decoder](bytes: Array[Byte]): Task[T] =
    ZIO.fromEither(parse(new String(bytes)).flatMap(js => js.as[T]))

}

object TestData {
  val Foobar1 = FooBar(1, Bar(Math.E, "hello", false))
  val Foobar2 = FooBar(2, Bar(Math.PI, "hello", true))

  val ListMessage = Message(
    Map("X-Request-Method" -> "GET", "X-Request-URL" -> "/foobar"),
    Array.emptyByteArray
  )

  def get(id: UUID) = Message(
    Map("X-Request-Method" -> "GET", "X-Request-URL" -> s"/foobar/${id.toString}"),
    Array.emptyByteArray
  )

  def add(foo: FooBar, meta: String = "must-be-present") = Message(
    Map("X-Request-Method" -> "POST", "X-Request-URL" -> s"/foobar/add", "X-Foobar-Meta" -> meta),
    foo.asJson.printWith(Printer.noSpaces).getBytes
  )
}
