import org.eigengo.build._

Build.Settings.root

scalaVersion := "2.11.2"

resolvers += "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases"

lazy val root = Project("lift-ml", file("."))
                  .configs(IntegrationTest)
                  .settings(Build.Settings.project:_*)

libraryDependencies ++= Seq(
  "org.typelevel" %% "scodec-core"      % "1.3.0",
  "org.scalanlp"  %% "breeze"           % "0.8.1",
  "org.scalanlp"  %% "breeze-natives"   % "0.8.1"
)
