import Dependencies._
import build._

Global / onChangedBuildSource := ReloadOnSourceChanges
// this doesn't work either
Global / testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework"))

lazy val service = Project(
  id = "service",
  base = file("service")
).settings(
  commonSettings,
  Seq(
    libraryDependencies ++= circe ++ tapir ++ `tapir-circe` ++ zio,
    libraryDependencies ++= `zio-test`
  )
)

lazy val `service-http4s` = Project(
  id = "service-http4s",
  base = file("service-http4s")
).settings(
  commonSettings,
  Seq(
    libraryDependencies ++= `slf4j-simple` ++ `tapir-redoc` ++ `tapir-server-http4s`,
    libraryDependencies ++= `zio-test`
  )
).dependsOn(service)

lazy val `service-message-based` = Project(
  id = "service-message-based",
  base = file("service-message-based")
).settings(
  commonSettings,
  Seq(
    libraryDependencies ++= `slf4j-simple` ++ `zio-test`
  )
).dependsOn(service)

lazy val `example-tapir-backend` = Project("example-tapir-backend", file("."))
  .settings(
    commonSettings,
    Seq(
      libraryDependencies ++= `zio-test` // this still doesn't work :/
    )
  )
  .aggregate(service, `service-http4s`, `service-message-based`)
