package is.solidninja.example.tapir.http4s

import is.solidninja.example.tapir._
import cats.syntax.semigroupk._
import org.http4s.HttpRoutes
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.Router
import sttp.tapir.redoc.bundle.RedocInterpreter
import sttp.tapir.server.http4s.ztapir.ZHttp4sServerInterpreter
import zio._
import zio.interop.catz._

object FooBarServer extends ZIOAppDefault {

  def routes: HttpRoutes[RIO[FooBarService.Environment, *]] =
    ZHttp4sServerInterpreter()
      .from(FooBarServerEndpoints.allRIO())
      .toRoutes

  def redocRoutes: HttpRoutes[RIO[FooBarService.Environment, *]] =
    ZHttp4sServerInterpreter()
      .from(
        RedocInterpreter()
          .fromEndpoints[RIO[FooBarService.Environment, *]](FooBarServiceEndpoints.All, "foobar service", "0.0.1")
      )
      .toRoutes

  def serve: RIO[FooBarService.Environment, Unit] =
    ZIO.executor.flatMap(executor =>
      BlazeServerBuilder[RIO[FooBarService.Environment, *]]
        .withExecutionContext(executor.asExecutionContext)
        .bindHttp(9090, "localhost")
        .withHttpApp(Router("/" -> (routes <+> redocRoutes)).orNotFound)
        .serve
        .compile
        .drain
    )

  def layer = FooBarRepository.stubLayer

  override def run =
    ZIO.logInfo("hello") *> serve.provide(layer).exitCode
}
