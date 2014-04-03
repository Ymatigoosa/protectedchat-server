import sbt._
import Keys._
import sbt.Keys._
import sbtassembly.Plugin._
import AssemblyKeys._
import com.typesafe.sbt.SbtStartScript

name := """tcp-async"""

version := "1.0"

scalaVersion := "2.10.4"

resolvers ++= Seq(
  "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/",
  "mandubian maven bintray" at "http://dl.bintray.com/mandubian/maven",
  "spray" at "http://repo.spray.io/"
)

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.3.0",
  "com.typesafe.akka" %% "akka-testkit" % "2.3.0",
  "com.github.mauricio" %% "postgresql-async" % "0.2.6",
  "io.spray" %%  "spray-json" % "1.2.5",
  "com.typesafe" % "config" % "1.0.2",
  "org.scalatest" % "scalatest_2.10" % "1.9.1" % "test",
  "io.spray" % "spray-can" % "1.3.1",
  "io.spray" % "spray-http" % "1.3.1",
  "io.spray" % "spray-httpx" % "1.3.1",
  "io.spray" % "spray-util" % "1.3.1",
  "io.spray" % "spray-client" % "1.3.1",
  "io.spray" % "spray-testkit" % "1.3.1",
  "io.spray" % "spray-routing" % "1.3.1",
  "org.mockito" % "mockito-core" % "1.9.5" % "test",
  "org.mockito" % "mockito-all" % "1.9.5" % "test"
)

seq(SbtStartScript.startScriptForClassesSettings: _*)

assemblySettings

test in assembly := {}