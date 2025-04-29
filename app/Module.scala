import javax.inject.{Inject, Singleton}

import scala.concurrent.Future

import com.google.inject.AbstractModule
import play.api.inject.ApplicationLifecycle
import play.api.{Configuration, Environment, Logger}
import service.RedisService

/** This class is a Guice module that tells Guice how to bind several different
  * types. This Guice module is created when the Play application starts.
  */
class Module(environment: Environment, configuration: Configuration)
    extends AbstractModule {
  override def configure(): Unit = {
    // Bind application lifecycle hook for Redis service
    bind(classOf[RedisLifecycleHook]).asEagerSingleton()
  }
}

/** Lifecycle hook for Redis connection pool management
  */
@Singleton
class RedisLifecycleHook @Inject() (
    lifecycle: ApplicationLifecycle,
    redisService: RedisService
) {
  private val logger = Logger(this.getClass)

  // Register shutdown hook to close Redis connections
  lifecycle.addStopHook { () =>
    logger.info("Shutting down Redis connections...")
    redisService.shutdown()
    Future.successful(())
  }

  logger.info("Redis lifecycle hook registered")
}
