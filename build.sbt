import scala.util.Properties.envOrElse

ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.7.0"

ThisBuild / semanticdbEnabled := true
ThisBuild / semanticdbVersion := scalafixSemanticdb.revision

val jacksonVersion = "2.19.1"
val tapirVersion = "1.11.36"

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .enablePlugins(DockerPlugin)
  .settings(
    name := "mixshare",
    Universal / javaOptions ++= Seq("-Dpidfile.path=/dev/null"),
    dockerBaseImage := "eclipse-temurin:21-alpine",
    dockerCommands := {
      // install useradd
      import com.typesafe.sbt.packager.docker._
      dockerCommands.value.foldLeft[Seq[CmdLike]](Nil) { (commands, command) =>
        commands ++ {
          command match {
            case Cmd("USER", "root") =>
              Seq(
                command,
                Cmd(
                  "RUN",
                  "apk add bash"
                )
              )
            case _ =>
              Seq(command)
          }
        }
      }
    },
    libraryDependencies ++= Seq(
      guice,
      evolutions,
      filters,
      "org.postgresql" % "postgresql" % "42.7.7",
      "org.playframework" %% "play-slick" % "6.2.0",
      "org.playframework" %% "play-slick-evolutions" % "6.2.0",
      "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.2" % Test,
      "org.typelevel" %% "cats-core" % "2.13.0",
      "com.auth0" % "java-jwt" % "4.5.0",
      "redis.clients" % "jedis" % "6.0.0",
      "com.h2database" % "h2" % "2.3.232" % Test,
      "org.mockito" % "mockito-core" % "5.18.0" % Test,
      // Tapir dependencies
      "com.softwaremill.sttp.tapir" %% "tapir-core" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-play-server" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-json-play" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-openapi-docs" % tapirVersion,
      "com.softwaremill.sttp.apispec" %% "openapi-circe-yaml" % "0.11.10",
      "io.circe" %% "circe-core" % "0.14.14",
      "io.circe" %% "circe-generic" % "0.14.14",
      "io.circe" %% "circe-parser" % "0.14.14"
    ),
    // Add routing settings for Scala 3
    scalacOptions ++= Seq("-Xignore-scala2-macros", "-source:3.7"),
    // Ensure routes file is properly handled
    routesImport += "controllers._",
    // Add this to correctly configure assets
    PlayKeys.playDefaultPort := 9000,
    dependencyOverrides ++= Seq(
      "com.fasterxml.jackson.core" % "jackson-databind" % jacksonVersion,
      "com.fasterxml.jackson.core" % "jackson-core" % jacksonVersion,
      "com.fasterxml.jackson.core" % "jackson-annotations" % jacksonVersion,
      "com.fasterxml.jackson.datatype" % "jackson-datatype-jdk8" % jacksonVersion,
      "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310" % jacksonVersion,
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % jacksonVersion,
      "com.fasterxml.jackson.module" % "jackson-module-parameter-names" % jacksonVersion
    )
  )
