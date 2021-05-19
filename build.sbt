name := "ergo-dex"

version := "0.1"

scalaVersion := "2.12.13"

idePackagePrefix := Some("io.ergodex")

libraryDependencies ++= Seq(
  "org.scalactic" %% "scalactic" % "3.2.7" % "test",
  "org.scalatest" %% "scalatest" % "3.2.7" % "test"
)
