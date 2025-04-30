package controllers

import java.time.ZonedDateTime
import javax.inject._

import scala.concurrent.{ExecutionContext, Future}

import actions.AuthAction
import models.{User, UserRepository}
import play.api.libs.json._
import play.api.mvc._
import service.AuthService

@Singleton
class UserController @Inject() (
    userRepository: UserRepository,
    authService: AuthService,
    authAction: AuthAction,
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

  // Registration request model
  case class RegistrationRequest(
      username: String,
      email: String,
      password: String
  )

  // Registration response model
  case class RegistrationResponse(
      success: Boolean,
      message: String,
      user: Option[SafeUser] = None
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

  // JSON formatters
  implicit val safeUserFormat: OFormat[SafeUser] = Json.format[SafeUser]
  implicit val registrationRequestFormat: Format[RegistrationRequest] =
    Json.format[RegistrationRequest]
  implicit val registrationResponseFormat: Format[RegistrationResponse] =
    Json.format[RegistrationResponse]

  // GET endpoint to retrieve a user by ID
  def getUser(id: Long): Action[AnyContent] = Action.async { implicit request =>
    userRepository.getById(id).map {
      case Some(user) => Ok(Json.toJson(toSafeUser(user)))
      case None =>
        NotFound(Json.obj("message" -> s"User with id $id not found"))
    }
  }

  // POST endpoint to register a new user
  def register(): Action[JsValue] = Action.async(parse.json) {
    implicit request =>
      val registerResult = request.body.validate[RegistrationRequest]

      registerResult.fold(
        errors => {
          val errorMessages = JsError.toJson(errors)
          Future.successful(
            BadRequest(
              Json.obj(
                "success" -> false,
                "message" -> "Invalid registration data",
                "errors" -> errorMessages
              )
            )
          )
        },
        registration => {
          // Input validation
          val validationErrors = validateRegistration(registration)
          if (validationErrors.nonEmpty) {
            Future.successful(
              BadRequest(
                Json.toJson(
                  RegistrationResponse(
                    success = false,
                    message = validationErrors.mkString(", ")
                  )
                )
              )
            )
          } else {
            // Register user
            authService
              .register(
                registration.username,
                registration.email,
                registration.password
              )
              .map {
                case Right(user) =>
                  Created(
                    Json.toJson(
                      RegistrationResponse(
                        success = true,
                        message = "User registered successfully",
                        user = Some(toSafeUser(user))
                      )
                    )
                  )
                case Left(errorMessage) =>
                  Conflict(
                    Json.toJson(
                      RegistrationResponse(
                        success = false,
                        message = errorMessage
                      )
                    )
                  )
              }
          }
        }
      )
  }

  // Validate registration input
  private def validateRegistration(
      registration: RegistrationRequest
  ): Seq[String] = {
    var errors = Seq.empty[String]

    // Username validation
    if (registration.username.trim.isEmpty) {
      errors = errors :+ "Username cannot be empty"
    } else if (registration.username.length < 3) {
      errors = errors :+ "Username must be at least 3 characters long"
    }

    // Email validation
    if (registration.email.trim.isEmpty) {
      errors = errors :+ "Email cannot be empty"
    } else if (
      !registration.email.matches(
        "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$"
      )
    ) {
      errors = errors :+ "Invalid email format"
    }

    // Password validation
    if (registration.password.trim.isEmpty) {
      errors = errors :+ "Password cannot be empty"
    } else if (registration.password.length < 6) {
      errors = errors :+ "Password must be at least 6 characters long"
    } else {
      // Check character types
      val hasLetters = registration.password.exists(_.isLetter)
      val hasDigits = registration.password.exists(_.isDigit)
      val hasSymbols = registration.password.exists(c => !c.isLetterOrDigit)

      val typesUsed = Seq(hasLetters, hasDigits, hasSymbols).count(_ == true)

      if (typesUsed < 3) {
        errors =
          errors :+ "Password must contain at least 3 different types of characters (letters, numbers, and symbols)"
      }
    }

    errors
  }

  // Protected endpoint - Get current user profile
  def getProfile: Action[AnyContent] = authAction.async { request =>
    // The user ID is available from the authenticated request
    val userId = request.userId

    userRepository.getById(userId).map {
      case Some(user) =>
        Ok(
          Json.toJson(
            Json.obj(
              "id" -> user.id,
              "username" -> user.username,
              "email" -> user.email,
              "createdAt" -> user.createdAt,
              "updatedAt" -> user.updatedAt
            )
          )
        )
      case None => NotFound(Json.obj("message" -> "User not found"))
    }
  }
}
