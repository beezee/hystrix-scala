name := """hxfn"""

version := "1.0"

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  "com.netflix.hystrix" % "hystrix-core" % "1.2.16",
  "org.scalatest" %% "scalatest" % "2.2.4" % "test"
)
