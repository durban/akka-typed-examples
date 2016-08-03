
updateOptions := updateOptions.value.withCachedResolution(true)

scalaVersion := "2.11.8"

scalacOptions ++= Seq(
  "-feature",
  "-deprecation",
  "-unchecked",
  "-encoding", "UTF-8",
  "-Xlint:_",
  "-Xfuture",
  //"-Xlog-implicits",
  "-Yno-adapted-args",
  "-Ywarn-numeric-widen",
  "-Ywarn-dead-code",
  "-Ywarn-unused-import"
)

val akkaVersion = "2.4.6"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
  "com.typesafe.akka" %% "akka-remote" % akkaVersion,
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
  "com.typesafe.akka" %% "akka-typed-experimental" % akkaVersion,
  "com.typesafe.akka" %% "akka-http-core" % akkaVersion,
  "com.typesafe.akka" %% "akka-http-experimental" % akkaVersion,

  "com.chuusai" %% "shapeless" % "2.3.1",
  "org.scodec" %% "scodec-bits" % "1.1.0",
  "org.scodec" %% "scodec-core" % "1.9.0",

  "org.scalatest" %% "scalatest" % "2.2.5" % "test",
  "org.scalacheck" %% "scalacheck" % "1.12.5" % "test",
  "junit" % "junit" % "4.12" % "test"
)

wartremoverWarnings ++= Seq(
  Wart.ImplicitConversion
)
