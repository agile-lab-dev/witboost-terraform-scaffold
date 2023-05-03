addSbtPlugin("com.dwijnand"   % "sbt-dynver"          % "4.1.1")
addSbtPlugin("org.scalameta"  % "sbt-scalafmt"        % "2.4.6")
addSbtPlugin("org.scoverage"  % "sbt-scoverage"       % "2.0.7")
addSbtPlugin("com.gilcloud"   % "sbt-gitlab"          % "0.1.2")
addSbtPlugin("com.github.sbt" % "sbt-native-packager" % "1.9.16")
addSbtPlugin("dev.guardrail"  % "sbt-guardrail"       % "0.75.1")

// sast job fails to build project
ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
