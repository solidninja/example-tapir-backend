package is.solidninja.example.tapir

import io.circe.generic.auto._
import sttp.tapir.PublicEndpoint
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.ztapir._
import sttp.tapir.server.ServerEndpoint
import zio._

import java.util.UUID

case class Bar(a: Double, b: String, c: Boolean)
case class FooBar(foo: Int, bar: Bar)

/** Domain error representation
  */
sealed trait Error

object Error {
  case object EmptyMetadata extends Error
  case class NotFound(id: UUID) extends Error
}

object FooBarServiceEndpoints {
  private val uuidBody = stringBody.map(UUID.fromString(_))(_.toString)
  private val errorBody = jsonBody[Error]

  val addFooBar: PublicEndpoint[(FooBar, String), Error, UUID, Any] =
    endpoint.post
      .in("foobar" / "add")
      .in(
        jsonBody[FooBar]
          .description("The foobar to add")
      )
      .in(
        header[String]("X-Foobar-Meta")
          .description("The metadata to add")
      )
      .out(uuidBody.description("identifier"))
      .errorOut(errorBody)

  val getFooBar: PublicEndpoint[UUID, Error, FooBar, Any] =
    endpoint.get
      .in("foobar" / path[UUID])
      .out(jsonBody[FooBar].description("The foobar, if found"))
      .errorOut(errorBody)

  val listFooBars: PublicEndpoint[Unit, Error, List[FooBar], Any] =
    endpoint.get
      .in("foobar")
      .out(jsonBody[List[FooBar]].description("All of the foobars"))
      .errorOut(errorBody)

  val All = List(addFooBar, getFooBar, listFooBars)
}

trait FooBarRepository {
  def store(value: FooBar): UIO[UUID]
  def list(): UIO[List[FooBar]]
  def get(id: UUID): UIO[Option[FooBar]]
}

object FooBarRepository {

  /** stub implementation using an in-memory map
    */
  def stubLayer: ZLayer[Any, Nothing, FooBarRepository] =
    ZLayer(Ref.make(Map.empty[UUID, FooBar]).map(stub))

  def stub(ref: Ref[Map[UUID, FooBar]]): FooBarRepository = new FooBarRepository {
    override def store(value: FooBar): UIO[UUID] =
      ZIO.succeed(UUID.randomUUID()).tap(uuid => ref.update(prev => prev + (uuid -> value)))

    override def list(): UIO[List[FooBar]] = ref.get.map(_.values.toList)

    override def get(id: UUID): UIO[Option[FooBar]] = ref.get.map(_.get(id))
  }

  def store(value: FooBar): URIO[FooBarRepository, UUID] = ZIO.serviceWithZIO(_.store(value))
  def list(): URIO[FooBarRepository, List[FooBar]] = ZIO.serviceWithZIO(_.list())
  def get(id: UUID): URIO[FooBarRepository, Option[FooBar]] = ZIO.serviceWithZIO(_.get(id))
}

object FooBarService {
  type Environment = FooBarRepository

  def store(foobar: FooBar, meta: String): ZIO[Environment, Error, UUID] =
    if (meta.isBlank) ZIO.fail(Error.EmptyMetadata)
    else FooBarRepository.store(foobar)

  def list(): ZIO[Environment, Error, List[FooBar]] =
    FooBarRepository.list()

  def get(id: UUID): ZIO[Environment, Error, FooBar] =
    FooBarRepository.get(id).someOrFail(Error.NotFound(id))
}

object FooBarServerEndpoints {

  def all(): List[ServerEndpoint[Any, URIO[FooBarService.Environment, *]]] = List(
    FooBarServiceEndpoints.addFooBar.serverLogic((FooBarService.store _).tupled.andThen(_.either)),
    FooBarServiceEndpoints.listFooBars.serverLogic(_ => FooBarService.list().either),
    FooBarServiceEndpoints.getFooBar.serverLogic(FooBarService.get(_).either)
  )

  // not particularly nice sadly
  def allRIO(): List[ZServerEndpoint[FooBarService.Environment, Any]] =
    all().asInstanceOf[List[ZServerEndpoint[FooBarService.Environment, Any]]]
}
