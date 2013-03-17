name := "syncissues"

version := "0.0.1"

organization := "fr.syncissues"

scalaVersion := "2.9.2"

seq(webSettings: _*)

resolvers ++= Seq("snapshots"     at "http://oss.sonatype.org/content/repositories/snapshots",
                "releases"        at "http://oss.sonatype.org/content/repositories/releases"
                )

scalacOptions ++= Seq("-deprecation", "-unchecked", "-optimise")

net.virtualvoid.sbt.graph.Plugin.graphSettings

libraryDependencies ++= {
  val liftVersion = "2.5-M3"
  Seq(
    "net.liftweb"             %% "lift-webkit"             % liftVersion        % "compile",
    "net.liftweb"             %% "lift-testkit"            % liftVersion        % "test",
    "net.liftmodules"         %% "lift-jquery-module"      % (liftVersion + "-2.0"),
    "net.liftmodules"         %% "fobo"                    % (liftVersion + "-0.7.9-SNAPSHOT"),
    "cc.co.scala-reactive"    %% "reactive-web"            % "0.3.0" excludeAll(ExclusionRule(organization = "net.liftweb")),
    "net.databinder.dispatch" %% "dispatch-core"           % "0.9.4",
    "net.databinder.dispatch" %% "dispatch-lift-json"      % "0.9.4" exclude("net.liftweb", "lift-json_2.9.1"),
    "org.scalaz"              %% "scalaz-core"             % "7.0.0-M7",
    "org.specs2"              %% "specs2"                  % "1.11"             % "test",
    "org.eclipse.jetty"        % "jetty-webapp"            % "8.1.9.v20130131"  % "container; compile",
    "org.functionaljava"       % "functionaljava"          % "3.1",
    "biz.futureware.mantis"    % "mantis-axis-soap-client" % "1.2.9",
    "ch.qos.logback"           % "logback-classic"         % "1.0.6"
  )
}
