import sbt._
trait Dependencies {

  lazy val testDependencies: Seq[ModuleID] = Seq(
    "org.scalactic"       %% "scalactic" % "3.2.12",
    "org.scalatest"       %% "scalatest" % "3.2.12" % Test,
    "com.lihaoyi"         %% "requests"  % "0.7.1"  % Test,
    "com.google.code.gson" % "gson"      % "2.9.0"  % Test
  )

  lazy val frameworkDependencies: Seq[ModuleID] = Seq(
    "io.circe"                  %% "circe-yaml"       % "0.14.2",
    "com.typesafe"               % "config"           % "1.2.1",
    "com.lihaoyi"               %% "requests"         % "0.7.1",
    "com.google.code.gson"       % "gson"             % "2.9.0",
    "org.slf4j"                  % "slf4j-api"        % "2.0.7",
    "ch.qos.logback"             % "logback-core"     % "1.4.6",
    "ch.qos.logback"             % "logback-classic"  % "1.4.6",
    "com.jayway.jsonpath"        % "json-path"        % "2.8.0",
    "com.fasterxml.jackson.core" % "jackson-databind" % "2.15.2"
  )

  private val http4sVersion                  = "0.23.18"
  lazy val http4sDependencies: Seq[ModuleID] = Seq(
    "org.http4s" %% "http4s-ember-client",
    "org.http4s" %% "http4s-ember-server",
    "org.http4s" %% "http4s-dsl",
    "org.http4s" %% "http4s-circe"
  ).map(_ % http4sVersion)

  private val circeVersion                   = "0.14.5"
  lazy val circeDependencies: Seq[ModuleID]  = Seq(
    "io.circe" %% "circe-core",
    "io.circe" %% "circe-generic",
    "io.circe" %% "circe-parser"
  ).map(_ % circeVersion) ++ Seq("io.circe" %% "circe-generic-extras" % "0.14.3")

}

object Dependencies extends Dependencies
