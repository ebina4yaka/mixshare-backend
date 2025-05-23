package models

import java.time.ZonedDateTime
import javax.inject.{Inject, Singleton}

import scala.concurrent.{ExecutionContext, Future}

import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import slick.jdbc.PostgresProfile.api._

@Singleton
class UserPasswordRepository @Inject() (
    protected val dbConfigProvider: DatabaseConfigProvider
)(implicit ec: ExecutionContext)
    extends HasDatabaseConfigProvider[JdbcProfile] {

  // UserPasswords table definition
  class UserPasswordsTable(tag: Tag)
      extends Table[UserPassword](tag, "user_passwords") {
    import ZonedDateTimeUtils.* // Import the ZonedDateTime column mapper

    def userId = column[Long]("user_id", O.PrimaryKey)
    def passwordHash = column[String]("password_hash")
    def createdAt = column[ZonedDateTime]("created_at")
    def updatedAt = column[ZonedDateTime]("updated_at")

    def * = (
      userId,
      passwordHash,
      createdAt,
      updatedAt
    ) <> (UserPassword.apply.tupled, UserPassword.unapply)
  }

  val userPasswords = TableQuery[UserPasswordsTable]

  // Find user password by user ID
  def findByUserId(userId: Long): Future[Option[UserPassword]] = {
    val query = userPasswords.filter(_.userId === userId)
    db.run(query.result.headOption)
  }

  // Create or update a user password
  def createOrUpdate(userPassword: UserPassword): Future[UserPassword] = {
    val upsertAction = userPasswords.insertOrUpdate(userPassword)
    db.run(upsertAction).map(_ => userPassword)
  }

  // Create a new user password
  def create(userPassword: UserPassword): Future[UserPassword] = {
    db.run(userPasswords += userPassword).map(_ => userPassword)
  }

  // Update an existing user password
  def update(userPassword: UserPassword): Future[Int] = {
    val query = userPasswords.filter(_.userId === userPassword.userId)
    db.run(query.update(userPassword))
  }
}
