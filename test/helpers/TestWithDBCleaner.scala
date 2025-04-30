package helpers

import scala.concurrent.Await
import scala.concurrent.duration._

import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

/** Trait to be mixed into test classes that need database cleaning before each
  * test
  */
trait TestWithDBCleaner
    extends TestHelpers
    with GuiceOneAppPerSuite
    with BeforeAndAfterEach {

  // Timeout for database operations
  private val timeout = 5.seconds

  /** Cleans the database before each test
    */
  override def beforeEach(): Unit = {
    super.beforeEach()
    val dbCleaner = app.injector.instanceOf[DatabaseCleaner]
    Await.result(dbCleaner.cleanDatabase(), timeout)
  }
}
