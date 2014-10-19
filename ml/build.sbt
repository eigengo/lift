// import org.eigengo.build._

scalaVersion := "2.11.2"

resolvers += "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases"

lazy val root = Project("lift-ml", file("."))
                  .configs(IntegrationTest)
                  //.settings(Build.Settings.project:_*)

libraryDependencies +=  "org.typelevel" %% "scodec-core" % "1.3.0"
