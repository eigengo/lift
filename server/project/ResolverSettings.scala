import sbt._

/**
 * Resolvers
 */
object ResolverSettings {
 
  /**
   * The local / development resolvers: it includes the default ones + Scala Linter.
   */
  lazy val resolvers = Seq(
    Resolver.defaultLocal,
    Resolver.mavenLocal,
    Resolver.sonatypeRepo("releases"),
    Resolver.typesafeRepo("releases"),
    Resolver.typesafeRepo("snapshots"),
    Resolver.sonatypeRepo("snapshots"),    
    "Linter" at "http://hairyfotr.github.io/linteRepo/releases",
    "krasserm" at "http://dl.bintray.com/krasserm/maven"
  )
}
