// We use sbt-dynver to auto-compute the version, so we MUST NOT set `version` here.
// See: https://github.com/sbt/sbt-dynver
ThisBuild / dynverSonatypeSnapshots := true // add `-SNAPSHOT` to versions from non-master branches
ThisBuild / dynverSeparator := "-"

ThisBuild / organization := "it.agilelab"
ThisBuild / scalaVersion := "2.13.8"
ThisBuild / scalacOptions := Seq("-unchecked", "-deprecation", "-Ywarn-unused", "-Ymacro-annotations")

// https://www.scala-sbt.org/1.x/docs/Publishing.html#Version+scheme
ThisBuild / versionScheme := Some("semver-spec")

ThisBuild / parallelExecution := false

lazy val framework = (project in file("framework"))
  .settings(
    name := "spin-framework",
    libraryDependencies ++= Dependencies.frameworkDependencies,
    libraryDependencies ++= Dependencies.testDependencies,
    libraryDependencies ++= Dependencies.http4sDependencies,
    libraryDependencies ++= Dependencies.circeDependencies
  )
  .settings(
    Compile / guardrailTasks := GuardrailHelpers.createGuardrailTasks((Compile / sourceDirectory).value / "openapi") {
      openApiFile =>
        List(
          ScalaServer(
            openApiFile.file,
            pkg = "it.agilelab.spinframework.app.api.generated",
            framework = "http4s",
            tracing = false
          )
        )
    },
    coverageExcludedPackages := "it.agilelab.spinframework.app.api.generated.*"
  )
  .dependsOn(principalmappingsamples)

lazy val spmock = (project in file("spmock"))
  .settings(
    name := "spin-spmock",
    libraryDependencies ++= Dependencies.testDependencies
  )
  .dependsOn(framework)
  .dependsOn(principalmappingsamples)

lazy val terraform = (project in file("terraform"))
  .settings(
    name := "terraform-provisioner",
    libraryDependencies ++= Dependencies.testDependencies,
    Compile / mainClass := Some("it.agilelab.provisioners.Main")
  )
  .enablePlugins(JavaAppPackaging)
  .dependsOn(framework)
  .dependsOn(principalmappingapi)

lazy val principalmappingapi     = (project in file("principalmapping-api"))
  .settings(
    name := "principalmapping-api",
    libraryDependencies ++= Dependencies.principalMappingPluginDependencies
  )
lazy val principalmappingsamples = (project in file("principalmapping-samples"))
  .settings(
    name := "principalmapping-samples",
    libraryDependencies ++= Dependencies.principalMappingPluginDependencies,
    libraryDependencies ++= Dependencies.testDependencies
  )
  .dependsOn(principalmappingapi)

lazy val principalmappingappmock = (project in file("principalmapping-app-mock"))
  .settings(
    name := "principalmapping-app-mock",
    libraryDependencies ++= Dependencies.testDependencies
  )
  .dependsOn(principalmappingapi)
  .dependsOn(principalmappingsamples)

lazy val root = (project in file("."))
  .aggregate(framework, spmock, terraform)
  .settings(
    name := "witboost-mesh-provisioning-terraform-specificprovisioner"
  )
