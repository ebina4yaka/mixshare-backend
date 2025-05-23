ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.6"

ThisBuild / semanticdbEnabled := true
ThisBuild / semanticdbVersion := scalafixSemanticdb.revision

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .settings(
    name := "ss-api",
    libraryDependencies ++= Seq(
      guice,
      evolutions,
      filters,
      "org.postgresql" % "postgresql" % "42.7.5",
      "org.playframework" %% "play-slick" % "6.2.0",
      "org.playframework" %% "play-slick-evolutions" % "6.2.0",
      "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.1" % Test,
      "org.typelevel" %% "cats-core" % "2.13.0",
      "com.auth0" % "java-jwt" % "4.5.0",
      "redis.clients" % "jedis" % "6.0.0",
      "com.h2database" % "h2" % "2.3.232" % Test,
      "org.mockito" % "mockito-core" % "5.17.0" % Test,
      "org.webjars" % "swagger-ui" % "5.21.0",
      "io.swagger.core.v3" % "swagger-core-jakarta" % "2.2.30",
      "io.swagger.parser.v3" % "swagger-parser" % "2.1.28",
      "com.fasterxml.jackson.core" % "jackson-databind" % "2.19.0" force (),
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.19.0" force ()
    ),
    // Add routing settings for Scala 3
    scalacOptions ++= Seq("-Xignore-scala2-macros", "-source:3.3"),
    // Ensure routes file is properly handled
    routesImport += "controllers._",
    // Add this to correctly configure assets
    PlayKeys.playDefaultPort := 9000,

    // Override Jackson dependencies for compatibility
    dependencyOverrides ++= Seq(
      "com.fasterxml.jackson.core" % "jackson-databind" % "2.19.0",
      "com.fasterxml.jackson.core" % "jackson-core" % "2.19.0",
      "com.fasterxml.jackson.core" % "jackson-annotations" % "2.19.0",
      "com.fasterxml.jackson.datatype" % "jackson-datatype-jdk8" % "2.19.0",
      "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310" % "2.19.0",
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.19.0",
      "com.fasterxml.jackson.module" % "jackson-module-parameter-names" % "2.19.0"
    )
  )
