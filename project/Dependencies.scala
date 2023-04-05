import sbt._
trait Dependencies {

  lazy val AkkaVersion     = "2.6.19"
  lazy val AkkaHttpVersion = "10.2.9"

  lazy val testDependencies: Seq[ModuleID] = Seq(
    "org.scalactic"       %% "scalactic" % "3.2.12",
    "org.scalatest"       %% "scalatest" % "3.2.12" % Test,
    "com.lihaoyi"         %% "requests"  % "0.7.1"  % Test,
    "com.google.code.gson" % "gson"      % "2.9.0"  % Test
  )

  lazy val frameworkDependencies: Seq[ModuleID] = Seq(
    "io.circe"            %% "circe-yaml" % "0.14.1",
    "com.typesafe"         % "config"     % "1.2.1",
    "com.lihaoyi"         %% "requests"   % "0.7.1",
    "com.google.code.gson" % "gson"       % "2.9.0"
  )

  lazy val akkaDependencies: Seq[ModuleID] =
    Seq(
      "com.typesafe.akka" %% "akka-actor-typed"     % AkkaVersion,
      "com.typesafe.akka" %% "akka-stream"          % AkkaVersion,
      "com.typesafe.akka" %% "akka-http"            % AkkaHttpVersion,
      "com.typesafe.akka" %% "akka-stream-testkit"  % AkkaVersion,
      "com.typesafe.akka" %% "akka-http-testkit"    % AkkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-spray-json" % AkkaHttpVersion
    )

}

object Dependencies extends Dependencies
