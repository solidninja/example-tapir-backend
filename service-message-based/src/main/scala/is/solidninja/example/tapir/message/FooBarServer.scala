package is.solidninja.example.tapir.message

import is.solidninja.example.tapir._
import zio._
import zio.stream.ZStream

object FooBarServer extends ZIOAppDefault {

  def server: MessageTapirBackend[FooBarService.Environment] =
    MessageTapirBackend[FooBarService.Environment](FooBarServerEndpoints.allRIO())

  // admittedly not very useful, hook it up to a real message broker
  def messageLoop: ZStream[Any, Nothing, Message] = ZStream.never

  def layer = FooBarRepository.stubLayer

  override def run =
    ZIO.logInfo("hello") *> messageLoop.mapZIO(server.handle).runDrain.provide(layer).exitCode
}
