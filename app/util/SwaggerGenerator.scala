package util

import java.io.{File, FileWriter}
import java.nio.file.{Files, Paths}
import javax.inject.{Inject, Singleton}

import scala.util.{Failure, Success, Try, Using}

import io.swagger.v3.core.util.Json
import io.swagger.v3.parser.OpenAPIV3Parser
import play.Environment
import play.api.{Configuration, Logger}

/** Utility for generating Swagger documentation
  */
@Singleton
class SwaggerGenerator @Inject() (
    environment: Environment,
    configuration: Configuration
) {
  private val logger = Logger(this.getClass)

  def generateSwaggerJson(): Unit = {
    val swaggerYmlPath =
      environment.getFile("/conf/swagger.yml").getAbsolutePath
    val publicDir = new File(environment.getFile("").getAbsolutePath, "public")
    val swaggerJsonPath = new File(publicDir, "swagger.json")

    if (!Files.exists(Paths.get(swaggerYmlPath))) {
      logger.error(s"Swagger YAML file not found at ${swaggerYmlPath}")
      return
    }

    // Create directories if they don't exist
    if (!publicDir.exists()) {
      publicDir.mkdirs()
    }

    logger.info(
      s"Converting Swagger YAML to JSON: ${swaggerYmlPath} -> ${swaggerJsonPath.getAbsolutePath}"
    )

    // Use the Swagger Parser library to properly parse YAML and convert to JSON
    Try {
      val openAPI = new OpenAPIV3Parser().read(swaggerYmlPath)
      Using(new FileWriter(swaggerJsonPath)) { writer =>
        writer.write(Json.pretty(openAPI))
      }
    } match {
      case Success(_) => logger.info("Swagger JSON generated successfully")
      case Failure(exception) =>
        logger.error("Failed to generate Swagger JSON", exception)
    }
  }
}
