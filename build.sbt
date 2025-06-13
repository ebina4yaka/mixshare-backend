ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.7.0"

ThisBuild / semanticdbEnabled := true
ThisBuild / semanticdbVersion := scalafixSemanticdb.revision

val jacksonVersion = "2.19.0"

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .settings(
    name := "ss-api",
    libraryDependencies ++= Seq(
      guice,
      evolutions,
      filters,
      "org.postgresql" % "postgresql" % "42.7.7",
      "org.playframework" %% "play-slick" % "6.2.0",
      "org.playframework" %% "play-slick-evolutions" % "6.2.0",
      "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.1" % Test,
      "org.typelevel" %% "cats-core" % "2.13.0",
      "com.auth0" % "java-jwt" % "4.5.0",
      "redis.clients" % "jedis" % "6.0.0",
      "com.h2database" % "h2" % "2.3.232" % Test,
      "org.mockito" % "mockito-core" % "5.18.0" % Test
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
