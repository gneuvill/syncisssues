name := "syncissues"

version := "0.0.1"

organization := "fr.syncissues"

scalaVersion := "2.10.3"

seq(webSettings: _*)

resolvers ++= Seq(
  "snapshots"     at "http://oss.sonatype.org/content/repositories/snapshots",
  "releases"      at "http://oss.sonatype.org/content/repositories/releases")

scalacOptions ++= Seq("-deprecation", "-unchecked", "-optimise", "-feature")

net.virtualvoid.sbt.graph.Plugin.graphSettings

libraryDependencies ++= {
  val liftVersion = "2.6"
  val liftMinVersion = "-M2"
  // val scalazVersion = "7.1.0-M4"
  val scalazVersion = "7.0.4"
  Seq(
    "net.liftweb"             %% "lift-webkit"                         % (liftVersion + liftMinVersion) % "compile",
    "net.liftweb"             %% "lift-testkit"                        % (liftVersion + liftMinVersion) % "test",
    "net.liftmodules"         %% ("lift-jquery-module_" + liftVersion) % "2.5",
    "net.liftmodules"         %% ("fobo_" + liftVersion)               % "1.1",
    "cc.co.scala-reactive"    %% "reactive-web"                        % "0.3.2.1" excludeAll(ExclusionRule(organization = "net.liftweb")),
    "net.databinder.dispatch" %% "dispatch-core"                       % "0.11.0",
    // "net.databinder.dispatch" %% "dispatch-lift-json"                  % "0.11.0" exclude("net.liftweb", "lift-json_2.10"),
    "io.argonaut"             %% "argonaut"                            % "6.0.2",
    "org.scalaz"              %% "scalaz-core"                         % scalazVersion,
    "org.scalaz"              %% "scalaz-concurrent"                   % scalazVersion,
    "org.specs2"              %% "specs2"                              % "2.3.8"                        % "test",
    "org.eclipse.jetty"        % "jetty-webapp"                        % "9.1.0.v20131115"              % "container; compile",
    "org.functionaljava"       % "functionaljava"                      % "3.1",
    "biz.futureware.mantis"    % "mantis-axis-soap-client"             % "1.2.9",
    "ch.qos.logback"           % "logback-classic"                     % "1.0.6"
  )
}
