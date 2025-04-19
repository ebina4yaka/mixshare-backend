import com.google.inject.AbstractModule
import play.api.{Configuration, Environment}

/** This class is a Guice module that tells Guice how to bind several
  * different types. This Guice module is created when the Play
  * application starts.
  */
class Module(environment: Environment, configuration: Configuration)
    extends AbstractModule {
  override def configure(): Unit = {
    // No custom bindings needed - using standard configuration
  }
}
