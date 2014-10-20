// import org.eigengo.build._

scalaVersion := "2.11.2"

resolvers += "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases"

lazy val root = Project("lift-ml", file("."))
                  .configs(IntegrationTest)
                  //.settings(Build.Settings.project:_*)

libraryDependencies  ++= Seq(
  "org.typelevel" %% "scodec-core" % "1.3.0",
  // other dependencies here
  "org.scalanlp" %% "breeze" % "0.8.1",
  // native libraries are not included by default. add this if you want them (as of 0.7)
  // native libraries greatly improve performance, but increase jar sizes.
  "org.scalanlp" %% "breeze-natives" % "0.8.1",
  "org.scalanlp" % "nak" % "1.2.1"
)
