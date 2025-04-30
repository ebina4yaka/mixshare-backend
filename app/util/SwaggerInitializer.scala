package util

import javax.inject.{Inject, Singleton}

import scala.concurrent.{ExecutionContext, Future}

import play.api.inject.ApplicationLifecycle
import play.api.{Configuration, Environment, Logger}

/** Initializes Swagger documentation when the application starts
  */
@Singleton
class SwaggerInitializer @Inject() (
    lifecycle: ApplicationLifecycle,
    swaggerGenerator: SwaggerGenerator
)(implicit ec: ExecutionContext) {

  private val logger = Logger(this.getClass)

  // Generate Swagger documentation
  logger.info("Initializing Swagger documentation...")
  swaggerGenerator.generateSwaggerJson()

  // Register shutdown hook
  lifecycle.addStopHook { () =>
    logger.info("SwaggerInitializer shutdown...")
    Future.successful(())
  }
}
