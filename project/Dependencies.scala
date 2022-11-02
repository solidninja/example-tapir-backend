import sbt._

object Dependencies {

  object Versions {
    val circe = "0.14.3"
    val http4s = "0.23.12"
    val slf4j = "1.7.36"
    val sttp = "3.8.3"
    val tapir = "1.1.4"
    val zio = "2.0.3"
    val `zio-cats` = "3.3.0"
  }

  val circe = Seq(
    "io.circe" %% "circe-core" % Versions.circe,
    "io.circe" %% "circe-generic" % Versions.circe,
    "io.circe" %% "circe-parser" % Versions.circe
  )

  val tapir = Seq(
    "com.softwaremill.sttp.client3" %% "zio" % Versions.sttp,
    "com.softwaremill.sttp.tapir" %% "tapir-core" % Versions.tapir,
    "com.softwaremill.sttp.tapir" %% "tapir-server" % Versions.tapir,
    "com.softwaremill.sttp.tapir" %% "tapir-zio" % Versions.tapir
  )

  val `tapir-circe` = Seq(
    "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % Versions.tapir
  )

  val `tapir-redoc` = Seq(
    "com.softwaremill.sttp.tapir" %% "tapir-redoc-bundle" % Versions.tapir
  )

  val `tapir-server-http4s` = Seq(
    "com.softwaremill.sttp.tapir" %% "tapir-http4s-server-zio" % Versions.tapir,
    "dev.zio" %% "zio-interop-cats" % Versions.`zio-cats`,
    "org.http4s" %% "http4s-blaze-server" % Versions.http4s
  )

  val `slf4j-simple` = Seq(
    "org.slf4j" % "slf4j-simple" % Versions.slf4j
  )

  val zio = Seq(
    "dev.zio" %% "zio" % Versions.zio
  )

  val `zio-test` = Seq(
    "dev.zio" %% "zio-test" % Versions.zio % "test",
    "dev.zio" %% "zio-test-junit" % Versions.zio % "test",
    "dev.zio" %% "zio-test-magnolia" % Versions.zio % "test",
    "dev.zio" %% "zio-test-sbt" % Versions.zio % "test"
  )

}
