name := "syncissues"

version := "0.0.1"

organization := "fr.syncissues"

scalaVersion := "2.9.2"

seq(webSettings: _*)

resolvers ++= Seq("snapshots"     at "http://oss.sonatype.org/content/repositories/snapshots",
                "releases"        at "http://oss.sonatype.org/content/repositories/releases"
                )

scalacOptions ++= Seq("-deprecation", "-unchecked")

libraryDependencies ++= {
  val liftVersion = "2.5-M3"
  Seq(
    "net.liftweb"             %% "lift-webkit"             % liftVersion        % "compile",
    "net.liftmodules"         %% "lift-jquery-module"      % (liftVersion + "-2.0"),
    "net.databinder.dispatch" %% "dispatch-core"           % "0.9.4",
    "net.databinder.dispatch" %% "dispatch-lift-json"      % "0.9.4",
    "org.specs2"              %% "specs2"                  % "1.11"             % "test",
    "org.eclipse.jetty"        % "jetty-webapp"            % "7.5.4.v20111024"  % "container; test",
    "org.functionaljava"       % "functionaljava"          % "3.1",
    "biz.futureware.mantis"    % "mantis-axis-soap-client" % "1.2.9",
    "ch.qos.logback"           % "logback-classic"         % "1.0.6"
  )
}
