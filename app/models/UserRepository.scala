package models

import java.time.ZonedDateTime
import javax.inject.{Inject, Singleton}

import scala.concurrent.{ExecutionContext, Future}

import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import slick.jdbc.PostgresProfile.api.*

@Singleton
class UserRepository @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext)
  extends HasDatabaseConfigProvider[JdbcProfile] {  // Import the ZonedDateTime column mapper

  // Users table definition
  class UsersTable(tag: Tag) extends Table[User](tag, "user") {
    import ZonedDateTimeUtils.*  // Import the ZonedDateTime column mapper
    
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def username = column[String]("username")
    def email = column[String]("email")
    def passwordHash = column[String]("password_hash")
    def createdAt = column[ZonedDateTime]("created_at")
    def updatedAt = column[ZonedDateTime]("updated_at")

    def * = (id.?, username, email, passwordHash, createdAt, updatedAt) <> ((User.apply _).tupled, User.unapply)
  }

  val users = TableQuery[UsersTable]

  // Get user by ID
  def getById(id: Long): Future[Option[User]] = {
    val query = users.filter(_.id === id)
    db.run(query.result.headOption)
  }
}
