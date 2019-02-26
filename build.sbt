import Dependencies._

def commonSettings(module: String) = Seq(
  name := module,
  organization := "fun.zhongl",
  version := "0.0.1",
  scalaVersion := "2.12.8",
  scalacOptions ++= Seq(
    "-deprecation",
    "-encoding",
    "UTF-8"
  )
)

lazy val root = (project in file("."))
  .settings(
    commonSettings("unix-domain-socket"),
    libraryDependencies ++= common ++ akka
  )

