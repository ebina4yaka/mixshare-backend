package api

import java.time.ZonedDateTime

import play.api.libs.json._
import sttp.tapir.Schema
import sttp.tapir.generic.auto._

object ApiTypes {

  // 認証関連の型定義
  case class LoginRequest(username: String, password: String)
  case class LoginResponse(
      success: Boolean,
      message: String,
      accessToken: Option[String] = None,
      refreshToken: Option[String] = None,
      userId: Option[Long] = None
  )

  case class RefreshTokenRequest(refreshToken: String)
  case class TokenRefreshResponse(
      success: Boolean,
      message: String,
      accessToken: Option[String] = None,
      refreshToken: Option[String] = None
  )

  case class LogoutRequest(refreshToken: String)
  case class LogoutResponse(success: Boolean, message: String)

  case class TokenVerificationResponse(
      valid: Boolean,
      userId: Option[String] = None,
      username: Option[String] = None,
      message: Option[String] = None
  )

  // ユーザー関連の型定義
  case class SafeUser(
      id: Option[Long],
      username: String,
      createdAt: ZonedDateTime,
      updatedAt: ZonedDateTime
  )

  case class RegistrationRequest(
      username: String,
      email: String,
      password: String
  )

  case class RegistrationResponse(
      success: Boolean,
      message: String,
      user: Option[SafeUser] = None
  )

  case class UserProfileResponse(
      id: Option[Long],
      username: String,
      email: String,
      createdAt: ZonedDateTime,
      updatedAt: ZonedDateTime
  )

  // レシピ関連の型定義
  case class Flavor(
      id: Option[Long],
      recipeId: Long,
      name: String,
      description: Option[String],
      createdAt: ZonedDateTime,
      updatedAt: ZonedDateTime
  )

  case class Recipe(
      id: Option[Long],
      name: String,
      description: Option[String],
      ingredients: String,
      instructions: String,
      cookingTime: Option[Int],
      servings: Option[Int],
      createdAt: ZonedDateTime,
      updatedAt: ZonedDateTime,
      flavors: Seq[Flavor] = Seq.empty
  )

  case class FindRecipesParams(
      name: Option[String] = None,
      flavorName: Option[String] = None,
      maxCookingTime: Option[Int] = None,
      minServings: Option[Int] = None
  )

  // エラーレスポンス
  case class ErrorResponse(message: String)
  case class NotFoundResponse(message: String)

  // JSON formatters for Play JSON
  implicit val loginRequestFormat: OFormat[LoginRequest] =
    Json.format[LoginRequest]
  implicit val loginResponseFormat: OFormat[LoginResponse] =
    Json.format[LoginResponse]
  implicit val refreshTokenRequestFormat: OFormat[RefreshTokenRequest] =
    Json.format[RefreshTokenRequest]
  implicit val tokenRefreshResponseFormat: OFormat[TokenRefreshResponse] =
    Json.format[TokenRefreshResponse]
  implicit val logoutRequestFormat: OFormat[LogoutRequest] =
    Json.format[LogoutRequest]
  implicit val logoutResponseFormat: OFormat[LogoutResponse] =
    Json.format[LogoutResponse]
  implicit val tokenVerificationResponseFormat
      : OFormat[TokenVerificationResponse] =
    Json.format[TokenVerificationResponse]

  implicit val safeUserFormat: OFormat[SafeUser] = Json.format[SafeUser]
  implicit val registrationRequestFormat: OFormat[RegistrationRequest] =
    Json.format[RegistrationRequest]
  implicit val registrationResponseFormat: OFormat[RegistrationResponse] =
    Json.format[RegistrationResponse]
  implicit val userProfileResponseFormat: OFormat[UserProfileResponse] =
    Json.format[UserProfileResponse]

  implicit val flavorFormat: OFormat[Flavor] = Json.format[Flavor]
  implicit val recipeFormat: OFormat[Recipe] = Json.format[Recipe]
  implicit val findRecipesParamsFormat: OFormat[FindRecipesParams] =
    Json.format[FindRecipesParams]

  implicit val errorResponseFormat: OFormat[ErrorResponse] =
    Json.format[ErrorResponse]
  implicit val notFoundResponseFormat: OFormat[NotFoundResponse] =
    Json.format[NotFoundResponse]

  // Tapir schemas
  implicit val loginRequestSchema: Schema[LoginRequest] =
    Schema.derived[LoginRequest]
  implicit val loginResponseSchema: Schema[LoginResponse] =
    Schema.derived[LoginResponse]
  implicit val refreshTokenRequestSchema: Schema[RefreshTokenRequest] =
    Schema.derived[RefreshTokenRequest]
  implicit val tokenRefreshResponseSchema: Schema[TokenRefreshResponse] =
    Schema.derived[TokenRefreshResponse]
  implicit val logoutRequestSchema: Schema[LogoutRequest] =
    Schema.derived[LogoutRequest]
  implicit val logoutResponseSchema: Schema[LogoutResponse] =
    Schema.derived[LogoutResponse]
  implicit val tokenVerificationResponseSchema
      : Schema[TokenVerificationResponse] =
    Schema.derived[TokenVerificationResponse]

  implicit val safeUserSchema: Schema[SafeUser] = Schema.derived[SafeUser]
  implicit val registrationRequestSchema: Schema[RegistrationRequest] =
    Schema.derived[RegistrationRequest]
  implicit val registrationResponseSchema: Schema[RegistrationResponse] =
    Schema.derived[RegistrationResponse]
  implicit val userProfileResponseSchema: Schema[UserProfileResponse] =
    Schema.derived[UserProfileResponse]

  implicit val flavorSchema: Schema[Flavor] = Schema.derived[Flavor]
  implicit val recipeSchema: Schema[Recipe] = Schema.derived[Recipe]
  implicit val findRecipesParamsSchema: Schema[FindRecipesParams] =
    Schema.derived[FindRecipesParams]

  implicit val errorResponseSchema: Schema[ErrorResponse] =
    Schema.derived[ErrorResponse]
  implicit val notFoundResponseSchema: Schema[NotFoundResponse] =
    Schema.derived[NotFoundResponse]
}
