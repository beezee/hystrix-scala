name := """hxfn"""

version := "1.0"

scalaVersion := "2.11.7"

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding", "UTF-8", // yes, this is 2 args
  "-feature",
  "-unchecked",
  "-Xfatal-warnings",
  "-Xlint",
  "-Yno-adapted-args",
  // "-Ywarn-dead-code", // N.B. doesn't work well with the ??? hole
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard",
  "-Xfuture")

libraryDependencies ++= Seq(
  "com.netflix.hystrix" % "hystrix-core" % "1.4.20",
  "org.scalaz" %% "scalaz-core" % "7.1.5",
  "org.scalacheck" %% "scalacheck" % "1.12.5" % "test",
  "org.slf4j" % "slf4j-log4j12" % "1.7.13" % "test" // really Hystrix?

)

initialCommands in console := """
  import bz._
  import bz.syntax.hx._
  import com.netflix.hystrix.HystrixCommandGroupKey
"""
