package is.solidninja.example.tapir.message

import sttp.monad.{Canceler, MonadAsyncError}
import zio.{IO, ZIO}

class ZIOMonadAsyncError[R, E](convertError: Throwable => IO[E, Nothing]) extends MonadAsyncError[ZIO[R, E, *]] {
  override def unit[T](t: T): ZIO[R, E, T] = ZIO.succeed(t)

  override def map[T, T2](fa: ZIO[R, E, T])(f: T => T2): ZIO[R, E, T2] = fa.map(f)

  override def flatMap[T, T2](fa: ZIO[R, E, T])(f: T => ZIO[R, E, T2]): ZIO[R, E, T2] =
    fa.flatMap(f)

  override def async[T](register: (Either[Throwable, T] => Unit) => Canceler): ZIO[R, E, T] =
    ZIO.asyncInterrupt { cb =>
      val canceler = register {
        case Left(t)  => cb(error(t))
        case Right(t) => cb(ZIO.succeed(t))
      }

      Left(ZIO.succeed(canceler.cancel()))
    }

  override def error[T](t: Throwable): ZIO[R, E, T] = convertError(t)

  override protected def handleWrappedError[T](rt: ZIO[R, E, T])(
      h: PartialFunction[Throwable, ZIO[R, E, T]]
  ): ZIO[R, E, T] =
    rt.catchSomeDefect(h) // todo: wonder if this does the right thing

  override def eval[T](t: => T): ZIO[R, E, T] =
    ZIO.succeed(t).catchSomeDefect(PartialFunction.fromFunction(convertError))

  override def flatten[T](ffa: ZIO[R, E, ZIO[R, E, T]]): ZIO[R, E, T] = ffa.flatten

  override def ensure[T](f: ZIO[R, E, T], e: => ZIO[R, E, Unit]): ZIO[R, E, T] = f.ensuring(e.ignore)
}
