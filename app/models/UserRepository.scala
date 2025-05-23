package models

import java.time.ZonedDateTime
import javax.inject.{Inject, Singleton}

import scala.concurrent.{ExecutionContext, Future}

import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import slick.jdbc.PostgresProfile.api._

@Singleton
class UserRepository @Inject() (
    protected val dbConfigProvider: DatabaseConfigProvider
)(implicit ec: ExecutionContext)
    extends HasDatabaseConfigProvider[JdbcProfile] {

  // Users table definition
  class UsersTable(tag: Tag) extends Table[User](tag, "user") {
    import ZonedDateTimeUtils.* // Import the ZonedDateTime column mapper

    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def username = column[String]("username")
    def email = column[String]("email")
    def createdAt = column[ZonedDateTime]("created_at")
    def updatedAt = column[ZonedDateTime]("updated_at")

    def * = (
      id.?,
      username,
      email,
      createdAt,
      updatedAt
    ) <> (User.apply.tupled, User.unapply)
  }

  val users = TableQuery[UsersTable]

  // Get user by ID
  def getById(id: Long): Future[Option[User]] = {
    val query = users.filter(_.id === id)
    db.run(query.result.headOption)
  }

  // Find user by username
  def findByUsername(username: String): Future[Option[User]] = {
    val query = users.filter(_.username === username)
    db.run(query.result.headOption)
  }

  // Find user by email
  def findByEmail(email: String): Future[Option[User]] = {
    val query = users.filter(_.email === email)
    db.run(query.result.headOption)
  }

  // Create a new user
  def create(user: User): Future[User] = {
    val insertQuery = users returning users.map(_.id) into ((user, id) =>
      user.copy(id = Some(id))
    )
    db.run(insertQuery += user)
  }
}
