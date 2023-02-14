name := "ergo-dex"

version := "0.1"

scalaVersion := "2.12.15"

scalacOptions ++= List(
  "-deprecation",
  "-encoding",
  "UTF-8",
  "-language:higherKinds",
  "-language:postfixOps",
  "-language:implicitConversions",
  "-feature",
  "-unchecked",
  "-Xfuture",
  "-Yno-adapted-args",
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard",
  "-Ypartial-unification"
)

libraryDependencies ++= Seq(
  "org.scorexfoundation"          %% "sigma-state"     % "4.0.5",
  "org.scalactic"                 %% "scalactic"       % "3.2.9"   % Test,
  "org.scalatest"                 %% "scalatest"       % "3.2.9"   % Test,
  "org.scalatestplus"             %% "scalacheck-1-15" % "3.2.9.0" % Test,
  "org.scalacheck"                %% "scalacheck"      % "1.15.4"  % Test,
  "org.typelevel"                 %% "cats-effect"     % "3.2.8"   % Test,
  "tf.tofu"                       %% "tofu"            % "0.10.8"  % Test,
  "tf.tofu"                       %% "derevo-circe"    % "0.13.0"  % Test,
  "com.softwaremill.sttp.client3" %% "circe"           % "3.3.18"  % Test,
  "com.softwaremill.sttp.client3" %% "okhttp-backend"  % "3.3.18"  % Test,
  compilerPlugin("org.typelevel" %% "kind-projector"     % "0.13.2" cross CrossVersion.full),
  compilerPlugin("com.olegpy"    %% "better-monadic-for" % "0.3.1")
)
