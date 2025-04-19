package controllers

import java.time.ZonedDateTime
import javax.inject.*

import scala.concurrent.ExecutionContext

import models.{User, UserRepository}
import play.api.libs.json.*
import play.api.mvc.*

@Singleton
class UserController @Inject() (
    userRepository: UserRepository,
    val controllerComponents: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BaseController {

  // Safe user representation without sensitive fields
  case class SafeUser(
      id: Option[Long],
      username: String,
      createdAt: ZonedDateTime,
      updatedAt: ZonedDateTime
  )

  // Convert User to SafeUser
  private def toSafeUser(user: User): SafeUser = {
    SafeUser(
      id = user.id,
      username = user.username,
      createdAt = user.createdAt,
      updatedAt = user.updatedAt
    )
  }

  // JSON formatter for SafeUser
  implicit val safeUserFormat: OFormat[SafeUser] = Json.format[SafeUser]

  // GET endpoint to retrieve a user by ID
  def getUser(id: Long): Action[AnyContent] = Action.async { implicit request =>
    userRepository.getById(id).map {
      case Some(user) => Ok(Json.toJson(toSafeUser(user)))
      case None =>
        NotFound(Json.obj("message" -> s"User with id $id not found"))
    }
  }
}
