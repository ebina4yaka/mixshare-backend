package util

import com.google.inject.AbstractModule
import play.api.{Configuration, Environment}

/** Guice module for Swagger-related bindings
  */
class SwaggerModule extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[SwaggerGenerator]).asEagerSingleton()
    bind(classOf[SwaggerInitializer]).asEagerSingleton()
  }
}
