import org.eigengo.build._

Build.Settings.root

lazy val root = Project("lift-ml", file("."))
                  .configs(IntegrationTest)
                  .settings(Build.Settings.project:_*)
