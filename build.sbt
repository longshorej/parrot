name := "parrot"

val Versions = {
  val Akka = "2.6.8"
  val AkkaHttp = "10.2.4"
}


libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % Akka,
  "com.typesafe.akka" %% "akka-stream" % Akka,
  "com.typesafe.akka" %% "akka-http" % AkkaHttp
)
