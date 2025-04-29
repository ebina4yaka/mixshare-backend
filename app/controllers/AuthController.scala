package controllers

import javax.inject._

import scala.concurrent.{ExecutionContext, Future}

import actions.AuthAction
import models.User
import play.api.libs.json._
import play.api.mvc._
import service.{AuthService, JwtService}

@Singleton
class AuthController @Inject() (
    authService: AuthService,
    jwtService: JwtService,
    authAction: AuthAction,
    val controllerComponents: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BaseController {

  // Login request model
  case class LoginRequest(username: String, password: String)

  // Login response model with refresh token
  case class LoginResponse(
      success: Boolean,
      message: String,
      accessToken: Option[String] = None,
      refreshToken: Option[String] = None,
      userId: Option[Long] = None
  )

  // JSON formatters
  implicit val loginRequestFormat: Format[LoginRequest] =
    Json.format[LoginRequest]
  implicit val loginResponseFormat: Format[LoginResponse] =
    Json.format[LoginResponse]

  // POST endpoint for authentication
  def login(): Action[JsValue] = Action.async(parse.json) { implicit request =>
    val loginResult = request.body.validate[LoginRequest]

    loginResult.fold(
      errors => {
        Future.successful(
          BadRequest(
            Json.obj(
              "success" -> false,
              "message" -> "Invalid request format"
            )
          )
        )
      },
      login => {
        authService.authenticate(login.username, login.password).flatMap {
          case Some(user) =>
            // Generate access and refresh tokens
            val accessToken = jwtService.createAccessToken(user)
            jwtService.createRefreshToken(user).map { refreshToken =>
              Ok(
                Json.toJson(
                  LoginResponse(
                    success = true,
                    message = "Authentication successful",
                    accessToken = Some(accessToken),
                    refreshToken = Some(refreshToken),
                    userId = user.id
                  )
                )
              )
            }
          case None =>
            Future.successful(
              Unauthorized(
                Json.toJson(
                  LoginResponse(
                    success = false,
                    message = "Invalid username or password"
                  )
                )
              )
            )
        }
      }
    )
  }

  // Verify token endpoint
  def verifyToken(): Action[AnyContent] = Action.async { request =>
    request.headers.get("Authorization") match {
      case Some(authHeader) if authHeader.startsWith("Bearer ") =>
        val token = authHeader.substring(7) // Remove "Bearer " prefix
        jwtService.verifyToken(token).map {
          case Some(claim) =>
            Ok(
              Json.obj(
                "valid" -> true,
                "userId" -> claim.subject,
                "username" -> claim.username
              )
            )
          case None =>
            Unauthorized(
              Json.obj(
                "valid" -> false,
                "message" -> "Invalid or expired token"
              )
            )
        }
      case _ =>
        Future.successful(
          BadRequest(
            Json.obj(
              "valid" -> false,
              "message" -> "Missing or invalid Authorization header"
            )
          )
        )
    }
  }

  // Logout response model
  case class LogoutResponse(
      success: Boolean,
      message: String
  )

  // JSON formatter for logout response
  implicit val logoutResponseFormat: Format[LogoutResponse] =
    Json.format[LogoutResponse]

  // Refresh token request model
  case class RefreshTokenRequest(refreshToken: String)

  // Response for token refresh
  case class TokenRefreshResponse(
      success: Boolean,
      message: String,
      accessToken: Option[String] = None,
      refreshToken: Option[String] = None
  )

  // JSON formatters for refresh token
  implicit val refreshTokenRequestFormat: Format[RefreshTokenRequest] =
    Json.format[RefreshTokenRequest]
  implicit val tokenRefreshResponseFormat: Format[TokenRefreshResponse] =
    Json.format[TokenRefreshResponse]

  // POST endpoint to refresh access token using a refresh token
  def refreshToken(): Action[JsValue] = Action.async(parse.json) {
    implicit request =>
      val refreshResult = request.body.validate[RefreshTokenRequest]

      refreshResult.fold(
        errors => {
          Future.successful(
            BadRequest(
              Json.obj(
                "success" -> false,
                "message" -> "Invalid request format"
              )
            )
          )
        },
        refresh => {
          jwtService.refreshTokens(refresh.refreshToken).map {
            case Some(tokenPair) =>
              Ok(
                Json.toJson(
                  TokenRefreshResponse(
                    success = true,
                    message = "Tokens refreshed successfully",
                    accessToken = Some(tokenPair.accessToken),
                    refreshToken = Some(tokenPair.refreshToken)
                  )
                )
              )
            case None =>
              Unauthorized(
                Json.toJson(
                  TokenRefreshResponse(
                    success = false,
                    message = "Invalid or expired refresh token"
                  )
                )
              )
          }
        }
      )
  }

  // POST endpoint for logout - invalidates both access and refresh tokens
  def logout(): Action[AnyContent] = Action.async { implicit request =>
    // Get access token from header
    val accessTokenOpt =
      request.headers.get("Authorization").map { authHeader =>
        if (authHeader.startsWith("Bearer ")) {
          authHeader.substring(7) // Remove "Bearer " prefix
        } else {
          authHeader
        }
      }

    // Get refresh token from body
    val refreshTokenOpt = request.body.asJson.flatMap { json =>
      (json \ "refreshToken").asOpt[String]
    }

    // Process both tokens
    val accessTokenFuture = accessTokenOpt match {
      case Some(token) => jwtService.invalidateAccessToken(token)
      case None => Future.successful(false) // No access token to invalidate
    }

    val refreshTokenFuture = refreshTokenOpt match {
      case Some(token) => jwtService.invalidateRefreshToken(token)
      case None => Future.successful(false) // No refresh token to invalidate
    }

    // Wait for both operations to complete
    for {
      accessResult <- accessTokenFuture
      refreshResult <- refreshTokenFuture
    } yield {
      if (accessResult && refreshResult) {
        Ok(
          Json.toJson(
            LogoutResponse(
              success = true,
              message = "Successfully logged out"
            )
          )
        )
      } else {
        InternalServerError(
          Json.toJson(
            LogoutResponse(
              success = false,
              message = "Failed to invalidate one or more tokens"
            )
          )
        )
      }
    }
  }
}
