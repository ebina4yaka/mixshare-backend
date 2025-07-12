package api

import api.ApiTypes._
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.json.play._

object UserApi {

  // Base API endpoint
  private val baseEndpoint = endpoint
    .in("api" / "users")
    .errorOut(statusCode and plainBody[String])

  // Get user by ID endpoint
  val getUserEndpoint = baseEndpoint.get
    .in(path[Long]("id"))
    .out(statusCode(StatusCode.Ok) and jsonBody[SafeUser])
    .errorOut(statusCode(StatusCode.NotFound) and jsonBody[NotFoundResponse])
    .name("getUser")
    .description("Get user by ID")
    .tag("Users")

  // User registration endpoint
  val registerEndpoint = endpoint
    .in("api" / "auth" / "register")
    .post
    .in(jsonBody[RegistrationRequest])
    .out(statusCode(StatusCode.Created) and jsonBody[RegistrationResponse])
    .errorOut(
      statusCode(StatusCode.BadRequest) and jsonBody[RegistrationResponse]
    )
    .errorOut(
      statusCode(StatusCode.Conflict) and jsonBody[RegistrationResponse]
    )
    .name("register")
    .description("Register a new user")
    .tag("Users")

  // Get user profile (protected endpoint)
  val getProfileEndpoint = baseEndpoint.get
    .in("profile")
    .in(auth.bearer[String]())
    .out(statusCode(StatusCode.Ok) and jsonBody[UserProfileResponse])
    .errorOut(statusCode(StatusCode.NotFound) and jsonBody[NotFoundResponse])
    .errorOut(statusCode(StatusCode.Unauthorized) and jsonBody[ErrorResponse])
    .name("getProfile")
    .description("Get current user profile (requires authentication)")
    .tag("Users")

  // All user endpoints
  val endpoints = List(
    getUserEndpoint,
    registerEndpoint,
    getProfileEndpoint
  )
}
