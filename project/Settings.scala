import io.taig.sbt.sonatype.SonatypeHouserulesPlugin.autoImport._
import sbt.{Def, Tests}
import sbt.Keys._
import sbt._

object Settings {
    val Scala211 = "2.11.12"

    val Scala212 = "2.12.6"

    val Scala213 = "2.13.1"
    
    val common = Def.settings(
        crossScalaVersions := Scala211 :: Scala212 :: Scala213 :: Nil,
        githubProject := "communicator",
        javacOptions ++= {
            scalaVersion.value match {
                case Scala211 =>
                    "-source" :: "1.7" ::
                    "-target" :: "1.7" ::
                    Nil
                case _ => Nil
            }
        },
        normalizedName := s"communicator-${normalizedName.value}",
        organization := "io.taig",
        scalacOptions ++=
            "-deprecation" ::
            "-feature" ::
            "-Xfatal-warnings" ::
            "-Ywarn-dead-code" ::
            "-Ywarn-numeric-widen" ::
            "-Ywarn-value-discard" ::
            Nil,
        scalaVersion := Scala213,
        testOptions in Test += Tests.Argument( "-oFD" )
    )
}