import sbt._
import Keys._

object build {

  val manifestSetting = packageOptions += {
    val (title, v, vendor) = (name.value, version.value, organization.value)
    Package.ManifestAttributes(
      "Created-By" -> "Simple Build Tool",
      "Built-By" -> System.getProperty("user.name"),
      "Build-Jdk" -> System.getProperty("java.version"),
      "Specification-Title" -> title,
      "Specification-Version" -> v,
      "Specification-Vendor" -> vendor,
      "Implementation-Title" -> title,
      "Implementation-Version" -> v,
      "Implementation-Vendor-Id" -> vendor,
      "Implementation-Vendor" -> vendor
    )
  }

  val commonSettings = Seq(
    organization := "is.solidninja.example",
    version := "0.0.1-SNAPSHOT",
    scalaVersion := "2.13.10",
    javacOptions ++= Seq("-target", "1.8", "-source", "1.8"),
    manifestSetting,
    crossVersion := CrossVersion.binary
  ) ++ addCompilerPlugin("org.typelevel" % "kind-projector" % "0.13.2" cross CrossVersion.full)
}
