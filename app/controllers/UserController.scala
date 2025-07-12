package controllers

import java.time.ZonedDateTime
import javax.inject._

import scala.concurrent.{ExecutionContext, Future}

import actions.AuthAction
import api.{ApiTypes, UserApi}
import models.{User, UserRepository}
import play.api.libs.json._
import play.api.mvc._
import service.AuthService
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.json.play._

@Singleton
class UserController @Inject() (
    userRepository: UserRepository,
    authService: AuthService,
    authAction: AuthAction,
    jwtService: service.JwtService,
    val controllerComponents: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BaseController {

  import ApiTypes._

  // Convert User to SafeUser
  private def toSafeUser(user: User): SafeUser = {
    SafeUser(
      id = user.id,
      username = user.username,
      createdAt = user.createdAt,
      updatedAt = user.updatedAt
    )
  }

  // Convert User to UserProfileResponse
  private def toUserProfileResponse(user: User): UserProfileResponse = {
    UserProfileResponse(
      id = user.id,
      username = user.username,
      email = user.email,
      createdAt = user.createdAt,
      updatedAt = user.updatedAt
    )
  }

  // GET endpoint to retrieve a user by ID
  def getUser(id: Long): Action[AnyContent] = Action.async { implicit request =>
    userRepository.getById(id).map {
      case Some(user) => Ok(Json.toJson(toSafeUser(user)))
      case None =>
        NotFound(Json.toJson(NotFoundResponse(s"User with id $id not found")))
    }
  }

  // POST endpoint to register a new user
  def register(): Action[AnyContent] = Action.async { implicit request =>
    request.body.asJson match {
      case Some(json) =>
        json.validate[RegistrationRequest] match {
          case JsSuccess(registrationRequest, _) =>
            // Input validation
            val validationErrors = validateRegistration(registrationRequest)
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
                  registrationRequest.username,
                  registrationRequest.email,
                  registrationRequest.password
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
          case JsError(errors) =>
            Future.successful(
              BadRequest(
                Json.toJson(
                  RegistrationResponse(
                    success = false,
                    message =
                      s"Invalid request format: ${errors.mkString(", ")}"
                  )
                )
              )
            )
        }
      case None =>
        Future.successful(
          BadRequest(
            Json.toJson(
              RegistrationResponse(
                success = false,
                message = "Request body must be JSON"
              )
            )
          )
        )
    }
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
  def getProfile: Action[AnyContent] = Action.async { implicit request =>
    request.headers.get("Authorization") match {
      case Some(authHeader) if authHeader.startsWith("Bearer ") =>
        val token = authHeader.substring(7)
        jwtService.verifyToken(token).flatMap {
          case Some(claim) =>
            val userId = claim.subject.toLong
            userRepository.getById(userId).map {
              case Some(user) =>
                Ok(Json.toJson(toUserProfileResponse(user)))
              case None =>
                NotFound(Json.toJson(NotFoundResponse("User not found")))
            }
          case None =>
            Future.successful(
              Unauthorized(
                Json.toJson(ErrorResponse("Invalid or expired token"))
              )
            )
        }
      case _ =>
        Future.successful(
          BadRequest(
            Json.toJson(
              ErrorResponse("Authorization header with Bearer token required")
            )
          )
        )
    }
  }

}
