package models

import java.sql.Timestamp
import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, ZoneId, ZoneOffset, ZonedDateTime}
import javax.inject.{Inject, Singleton}

import scala.concurrent.{ExecutionContext, Future}

import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.ast.BaseTypedType
import slick.jdbc.JdbcProfile
import slick.jdbc.PostgresProfile.api._
object ZonedDateTimeUtils {
  // Custom formatter that doesn't include zone ID (like [Asia/Tokyo])
  private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSX")
  
  // Store ZonedDateTime as Timestamp for PostgreSQL
  implicit val zonedDateTimeColumnType: BaseColumnType[ZonedDateTime] = MappedColumnType.base[ZonedDateTime, Timestamp](
    // To database: Convert ZonedDateTime to SQL Timestamp preserving the instant
    zonedDateTime => Timestamp.from(zonedDateTime.toInstant),
    
    // From database: Convert SQL Timestamp to ZonedDateTime using fixed offset
    timestamp => {
      val instant = timestamp.toInstant
      // Use ZoneOffset.ofHours(9) for Japan timezone without zone ID
      val zdt = ZonedDateTime.ofInstant(instant, ZoneOffset.ofHours(9))
      // Format and parse to remove zone ID
      ZonedDateTime.parse(zdt.format(formatter), formatter)
    }
  )
}

@Singleton
class UserRepository @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext)
  extends HasDatabaseConfigProvider[JdbcProfile] {

  // Users table definition
  class UsersTable(tag: Tag) extends Table[User](tag, "users") {
    import ZonedDateTimeUtils._  // Import the ZonedDateTime column mapper
    
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