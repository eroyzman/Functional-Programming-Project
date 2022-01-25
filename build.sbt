name := "key-finder"

version := "0.1"

scalaVersion := "2.13.7"

val AkkaVersion = "2.6.18"
val AkkaHttpVersion = "10.2.7"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
  "com.typesafe.akka" %% "akka-stream-typed" % AkkaVersion,
  "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
  "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-xml" % AkkaHttpVersion,
  "com.typesafe.play" %% "play-json" % "2.9.2",
  "com.typesafe.akka" %% "akka-serialization-jackson" % AkkaVersion,
  "com.typesafe.akka" %% "akka-stream-testkit" % AkkaVersion % Test,
  "org.mongodb.scala" %% "mongo-scala-driver" % "2.7.0",
  "ch.rasc" % "bsoncodec" % "1.0.1",
  "com.lightbend.akka" %% "akka-stream-alpakka-mongodb" % "2.0.2"
)

