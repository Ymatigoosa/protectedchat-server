import sbt._
import Keys._
import sbtassembly.Plugin._
import AssemblyKeys._

name := """tcp-async"""

version := "1.0"

scalaVersion := "2.10.4"

resolvers ++= Seq(
"Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"
)

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.3.0",
  "com.typesafe.akka" %% "akka-testkit" % "2.3.0",
  "com.github.mauricio" %% "postgresql-async" % "0.2.6",
  "com.typesafe" % "config" % "1.0.2",
  "org.scalatest" % "scalatest_2.10" % "1.9.1" % "test",
  "org.mockito" % "mockito-core" % "1.9.5" % "test",
  "org.mockito" % "mockito-all" % "1.9.5" % "test"
)

assemblySettings

test in assembly := {}