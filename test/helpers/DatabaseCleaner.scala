package helpers

import javax.inject.{Inject, Singleton}

import scala.concurrent.{ExecutionContext, Future}

import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.H2Profile.api._
import slick.jdbc.JdbcProfile

@Singleton
class DatabaseCleaner @Inject() (dbConfigProvider: DatabaseConfigProvider)(
    implicit ec: ExecutionContext
) {
  private val dbConfig = dbConfigProvider.get[JdbcProfile]
  private val db = dbConfig.db

  /** Clears all tables in the in-memory H2 database Note: This is only for use
    * in test environments
    */
  def cleanDatabase(): Future[Unit] = {
    val tables = Seq(
      sqlu"TRUNCATE TABLE users RESTART IDENTITY CASCADE",
      sqlu"TRUNCATE TABLE user_passwords RESTART IDENTITY CASCADE",
      sqlu"TRUNCATE TABLE recipes RESTART IDENTITY CASCADE"
      // Add any other tables that need to be cleaned
    )

    db.run(DBIO.sequence(tables).transactionally).map(_ => ())
  }
}
