name := "ergo-dex"

version := "0.1"

scalaVersion := "2.12.15"

libraryDependencies ++= Seq(
  "org.scorexfoundation" %% "sigma-state"     % "4.0.5",
  "org.scalactic"        %% "scalactic"       % "3.2.9"   % Test,
  "org.scalatest"        %% "scalatest"       % "3.2.9"   % Test,
  "org.scalatestplus"    %% "scalacheck-1-15" % "3.2.9.0" % Test,
  "org.scalacheck"       %% "scalacheck"      % "1.15.4"  % Test,
  "org.typelevel"        %% "cats-effect"     % "3.2.8"   % Test
)
