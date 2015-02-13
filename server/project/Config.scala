import sbt._
import Keys._

object Config {

  lazy val LiftLocalApp = config("local-app") extend Compile
  lazy val LiftContainerApp = config("container-app") extend Compile

}