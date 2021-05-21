name := "ergo-dex"

version := "0.1"

scalaVersion := "2.12.13"

libraryDependencies ++= Seq(
  "org.scalactic"     %% "scalactic"       % "3.2.9"   % "test",
  "org.scalatest"     %% "scalatest"       % "3.2.9"   % "test",
  "org.scalatestplus" %% "scalacheck-1-15" % "3.2.9.0" % "test",
  "org.scalacheck"    %% "scalacheck"      % "1.15.4"  % "test"
)
