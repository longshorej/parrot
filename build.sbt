name := "parrot"

resolvers += Resolver.JCenterRepository

val Versions = new {
  val Ackord    = "0.17.1"
  val Akka      = "2.6.8"
  val AkkaHttp  = "10.2.4"
  val ScalaTest = "3.2.9"
  val Slf4j     = "1.7.30"
  val SprayJson = "1.3.6"
}

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed"  % Versions.Akka,
  "com.typesafe.akka" %% "akka-stream"       % Versions.Akka,
  "com.typesafe.akka" %% "akka-stream-typed" % Versions.Akka,
  "com.typesafe.akka" %% "akka-http"         % Versions.AkkaHttp,
  "io.spray"          %% "spray-json"        % Versions.SprayJson,
  "net.katsstuff"     %% "ackcord"           % Versions.Ackord,
  "org.slf4j"          % "slf4j-simple"      % Versions.Slf4j,


  "org.scalatest"     %% "scalatest"        % "3.2.9"                %      "test"
)

scalacOptions ++= Seq(
  "-encoding", "utf8",
  "-Xfatal-warnings",
  "-deprecation"
)

