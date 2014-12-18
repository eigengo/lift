import sbt._
import Keys._

/**
 * This object includes the publishing mechanism. We simply publish to the ``artifacts`` directory,
 * which Jenkins build uses to automatically push the built artefacts to Artifactory.
 */
object PublishSettings {

  lazy val publishSettings: Seq[Def.Setting[_]] = Seq(
    publishArtifact in (Compile, packageDoc) := false,
    publishMavenStyle := true,
    pomIncludeRepository := { _ => false },
    publishTo := {
      val nexus = "https://oss.sonatype.org/"
      if (isSnapshot.value)
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases"  at nexus + "service/local/staging/deploy/maven2")
    },
    pomExtra := (
      <url>https://github.com/eigengo</url>
      <licenses>
        <license>
          <name>BSD-style</name>
          <url>http://www.opensource.org/licenses/bsd-license.php</url>
          <distribution>repo</distribution>
        </license>
      </licenses>
      <scm>
        <url>git@github.com:eigengo/lift.git</url>
        <connection>scm:git:git@github.com:eigengo/lift.git</connection>
      </scm>
      <developers>
        <developer>
          <id>janm399</id>
          <name>Jan Machacek</name>
          <url>http://www.eigengo.com</url>
        </developer>
      </developers>),
    credentials ++= (for {
      username <- Option(System.getenv().get("SONATYPE_USERNAME"))
      password <- Option(System.getenv().get("SONATYPE_PASSWORD"))
    } yield Credentials("Sonatype Nexus Repository Manager", "oss.sonatype.org", username, password)).toSeq
  )
  
}
