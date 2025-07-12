package controllers

import javax.inject._

import scala.concurrent.{ExecutionContext, Future}

import actions.AuthAction
import api.{ApiTypes, AuthApi}
import models.User
import play.api.libs.json._
import play.api.mvc._
import service.{AuthService, JwtService}
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.json.play._

@Singleton
class AuthController @Inject() (
    authService: AuthService,
    jwtService: JwtService,
    authAction: AuthAction,
    val controllerComponents: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BaseController {

  import ApiTypes._

  // POST endpoint for authentication
  def login(): Action[AnyContent] = Action.async { implicit request =>
    request.body.asJson match {
      case Some(json) =>
        json.validate[LoginRequest] match {
          case JsSuccess(loginRequest, _) =>
            authService
              .authenticate(loginRequest.username, loginRequest.password)
              .flatMap {
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
          case JsError(errors) =>
            Future.successful(
              BadRequest(
                Json.toJson(
                  LoginResponse(
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
              LoginResponse(
                success = false,
                message = "Request body must be JSON"
              )
            )
          )
        )
    }
  }

  // Verify token endpoint
  def verifyToken(): Action[AnyContent] = Action.async { implicit request =>
    request.headers.get("Authorization") match {
      case Some(authHeader) if authHeader.startsWith("Bearer ") =>
        val token = authHeader.substring(7)
        jwtService.verifyToken(token).map {
          case Some(claim) =>
            Ok(
              Json.toJson(
                TokenVerificationResponse(
                  valid = true,
                  userId = Some(claim.subject),
                  username = Some(claim.username)
                )
              )
            )
          case None =>
            Unauthorized(
              Json.toJson(
                TokenVerificationResponse(
                  valid = false,
                  message = Some("Invalid or expired token")
                )
              )
            )
        }
      case _ =>
        Future.successful(
          BadRequest(
            Json.toJson(
              TokenVerificationResponse(
                valid = false,
                message =
                  Some("Authorization header with Bearer token required")
              )
            )
          )
        )
    }
  }

  // POST endpoint to refresh access token using a refresh token
  def refreshToken(): Action[AnyContent] = Action.async { implicit request =>
    request.body.asJson match {
      case Some(json) =>
        json.validate[RefreshTokenRequest] match {
          case JsSuccess(refreshRequest, _) =>
            jwtService.refreshTokens(refreshRequest.refreshToken).map {
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
          case JsError(errors) =>
            Future.successful(
              BadRequest(
                Json.toJson(
                  TokenRefreshResponse(
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
              TokenRefreshResponse(
                success = false,
                message = "Request body must be JSON"
              )
            )
          )
        )
    }
  }

  // POST endpoint for logout - invalidates both access and refresh tokens
  def logout(): Action[AnyContent] = Action.async { implicit request =>
    (request.body.asJson, request.headers.get("Authorization")) match {
      case (Some(json), Some(authHeader)) if authHeader.startsWith("Bearer ") =>
        val accessToken = authHeader.substring(7)
        json.validate[LogoutRequest] match {
          case JsSuccess(logoutRequest, _) =>
            // Process both tokens
            val accessTokenFuture =
              jwtService.invalidateAccessToken(accessToken)
            val refreshTokenFuture =
              jwtService.invalidateRefreshToken(logoutRequest.refreshToken)

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
          case JsError(errors) =>
            Future.successful(
              BadRequest(
                Json.toJson(
                  LogoutResponse(
                    success = false,
                    message =
                      s"Invalid request format: ${errors.mkString(", ")}"
                  )
                )
              )
            )
        }
      case _ =>
        Future.successful(
          BadRequest(
            Json.toJson(
              LogoutResponse(
                success = false,
                message =
                  "Request body (JSON) and Authorization header with Bearer token required"
              )
            )
          )
        )
    }
  }
}
