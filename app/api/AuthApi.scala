package api

import api.ApiTypes._
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.json.play._

object AuthApi {

  // Base API endpoint
  private val baseEndpoint = endpoint
    .in("api" / "auth")
    .errorOut(statusCode and plainBody[String])

  // Login endpoint
  val loginEndpoint = baseEndpoint.post
    .in("login")
    .in(jsonBody[LoginRequest])
    .out(statusCode(StatusCode.Ok) and jsonBody[LoginResponse])
    .errorOut(statusCode(StatusCode.BadRequest) and jsonBody[LoginResponse])
    .errorOut(statusCode(StatusCode.Unauthorized) and jsonBody[LoginResponse])
    .name("login")
    .description("Authenticate user with username and password")
    .tag("Authentication")

  // Verify token endpoint
  val verifyTokenEndpoint = baseEndpoint.get
    .in("verify")
    .in(auth.bearer[String]())
    .out(statusCode(StatusCode.Ok) and jsonBody[TokenVerificationResponse])
    .errorOut(
      statusCode(StatusCode.BadRequest) and jsonBody[TokenVerificationResponse]
    )
    .errorOut(
      statusCode(StatusCode.Unauthorized) and jsonBody[
        TokenVerificationResponse
      ]
    )
    .name("verifyToken")
    .description("Verify JWT access token")
    .tag("Authentication")

  // Refresh token endpoint
  val refreshTokenEndpoint = baseEndpoint.post
    .in("refresh")
    .in(jsonBody[RefreshTokenRequest])
    .out(statusCode(StatusCode.Ok) and jsonBody[TokenRefreshResponse])
    .errorOut(
      statusCode(StatusCode.BadRequest) and jsonBody[TokenRefreshResponse]
    )
    .errorOut(
      statusCode(StatusCode.Unauthorized) and jsonBody[TokenRefreshResponse]
    )
    .name("refreshToken")
    .description("Refresh access token using refresh token")
    .tag("Authentication")

  // Logout endpoint
  val logoutEndpoint = baseEndpoint.post
    .in("logout")
    .in(jsonBody[LogoutRequest])
    .in(auth.bearer[String]())
    .out(statusCode(StatusCode.Ok) and jsonBody[LogoutResponse])
    .errorOut(statusCode(StatusCode.BadRequest) and jsonBody[LogoutResponse])
    .errorOut(
      statusCode(StatusCode.InternalServerError) and jsonBody[LogoutResponse]
    )
    .name("logout")
    .description("Logout user and invalidate tokens")
    .tag("Authentication")

  // All auth endpoints
  val endpoints = List(
    loginEndpoint,
    verifyTokenEndpoint,
    refreshTokenEndpoint,
    logoutEndpoint
  )
}
