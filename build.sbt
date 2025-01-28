ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.3"

lazy val root = (project in file("."))
  .settings(
    name := "typeracer"
  )

libraryDependencies += "org.jsoup" % "jsoup" % "1.17.2"
libraryDependencies += "org.scalafx" % "scalafx_3" % "20.0.0-R31"
//libraryDependencies += "org.scala-lang.modules" %% "scala-swing" % "3.0.0"
//libraryDependencies += "org.openjfx" % "javafx-controls" % "22.0.1"