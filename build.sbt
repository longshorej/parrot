enablePlugins(JavaServerAppPackaging)

name := "parrot"

resolvers += Resolver.JCenterRepository

val Versions = new {
  val Ackord       = "0.17.1"
  val Akka         = "2.6.8"
  val AkkaHttp     = "10.2.4"
  val Logback      = "1.2.12"
  val PPrint       = "0.7.0"
  val Scala        = "2.13.12"
  val ScalaLogging = "3.9.4"
  val ScalaTest    = "3.2.9"
  val SprayJson    = "1.3.6"
}

libraryDependencies ++= Seq(
  "com.lihaoyi"                %% "pprint"            % Versions.PPrint,
  "com.typesafe.scala-logging" %% "scala-logging"     % Versions.ScalaLogging,
  "com.typesafe.akka"          %% "akka-actor-typed"  % Versions.Akka,
  "com.typesafe.akka"          %% "akka-slf4j"        % Versions.Akka,
  "com.typesafe.akka"          %% "akka-stream"       % Versions.Akka,
  "com.typesafe.akka"          %% "akka-stream-typed" % Versions.Akka,
  "com.typesafe.akka"          %% "akka-http"         % Versions.AkkaHttp,
  "io.spray"                   %% "spray-json"        % Versions.SprayJson,
  "net.katsstuff"              %% "ackcord"           % Versions.Ackord,

  "ch.qos.logback"              % "logback-classic"   % Versions.Logback,

  "org.scalatest"              %% "scalatest"         % Versions.ScalaTest               %      "test"
)

reStart / envVars := List("PARROT_DAD_JOKES_PATH")
  .flatMap(n => sys.env.get(n).map(v => n -> v))
  .toMap

scalaVersion := Versions.Scala

scalacOptions ++= Seq(
  "-encoding", "utf8",
  "-Xfatal-warnings",
  "-deprecation"
)

