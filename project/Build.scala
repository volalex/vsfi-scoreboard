import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "vsfi-scoreboard"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    // Add your project dependencies here,
    jdbc,
    anorm,
    "eu.henkelmann" % "actuarius_2.10.0" % "0.2.6"
  )




  val main = play.Project(appName, appVersion, appDependencies).settings(
    scalacOptions ++= Seq("-deprecation","-feature","-language:postfixOps")
  )

}
