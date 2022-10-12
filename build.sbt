name := "exampleLoad"

version := "1.0"

scalaVersion := "2.10.4"

enablePlugins(GatlingPlugin)

libraryDependencies += "io.gatling.highcharts" % "gatling-charts-highcharts" % "2.2.2" % "test"

libraryDependencies += "io.gatling" % "gatling-test-framework" % "2.2.2" % "test"

