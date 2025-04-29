package models

import java.time.ZonedDateTime

import play.api.libs.json.{Format, Json, OFormat, Reads, Writes}

// Test models for use in tests
case class LoginRequestTest(username: String, password: String)
case class RefreshTokenRequestTest(refreshToken: String)
case class RegistrationRequestTest(
    username: String,
    email: String,
    password: String
)
case class LoginResponseTest(
    success: Boolean,
    message: String,
    accessToken: Option[String] = None,
    refreshToken: Option[String] = None,
    userId: Option[Long] = None
)
// This should match the structure in the real service
case class TokenPairTest(accessToken: String, refreshToken: String)

object ModelFormatters {
  // For ZonedDateTime handling
  import java.time.format.DateTimeFormatter

  implicit val zonedDateTimeReads: Reads[ZonedDateTime] =
    Reads.of[String].map(ZonedDateTime.parse)
  implicit val zonedDateTimeWrites: Writes[ZonedDateTime] = Writes
    .of[String]
    .contramap(_.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))

  // Test model formats
  implicit val loginRequestFormat: Format[LoginRequestTest] =
    Json.format[LoginRequestTest]
  implicit val refreshTokenRequestFormat: Format[RefreshTokenRequestTest] =
    Json.format[RefreshTokenRequestTest]
  implicit val loginResponseFormat: Format[LoginResponseTest] =
    Json.format[LoginResponseTest]
  implicit val registrationRequestFormat: Format[RegistrationRequestTest] =
    Json.format[RegistrationRequestTest]
  implicit val tokenPairTestFormat: Format[TokenPairTest] =
    Json.format[TokenPairTest]

  // No custom reads needed
}
