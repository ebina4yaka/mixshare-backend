package controllers

import java.time.ZonedDateTime

import scala.concurrent.Future

import helpers.TestWithDBCleaner
import models.{LoginRequestTest, RefreshTokenRequestTest, User}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.api.test._
import service.{AuthService, JwtService, TokenBlacklistService}

class AuthControllerSpec
    extends TestWithDBCleaner
    with GuiceOneAppPerSuite
    with MockitoSugar
    with BeforeAndAfterEach {

  val mockAuthService: AuthService = mock[AuthService]
  val mockJwtService: JwtService = mock[JwtService]
  val mockTokenBlacklistService: TokenBlacklistService =
    mock[TokenBlacklistService]

  override def fakeApplication(): Application = {
    GuiceApplicationBuilder()
      .overrides(
        bind[AuthService].toInstance(mockAuthService),
        bind[JwtService].toInstance(mockJwtService),
        bind[TokenBlacklistService].toInstance(mockTokenBlacklistService),
        bind[services.MockModule].toInstance(new services.MockModule)
      )
      .build()
  }

  override def beforeEach(): Unit = {
    reset(mockAuthService, mockJwtService, mockTokenBlacklistService)
  }

  "AuthController login" should {
    "return 200 and login response when authentication is successful" in {
      val loginRequest = LoginRequestTest("testuser", "password123")
      val now = ZonedDateTime.now()
      val user =
        User(Some(1L), "testuser", "test@example.com", now, now)

      when(mockAuthService.authenticate(any[String], any[String]))
        .thenReturn(Future.successful(Some(user)))
      when(mockJwtService.createAccessToken(any[User]))
        .thenReturn("test.access.token")
      when(mockJwtService.createRefreshToken(any[User]))
        .thenReturn(Future.successful("test.refresh.token"))

      val request = FakeRequest(POST, "/api/auth/login")
        .withHeaders("Csrf-Token" -> "nocheck")
        .withJsonBody(
          Json.toJson(loginRequest)(models.ModelFormatters.loginRequestFormat)
        )

      val result = route(app, request).get

      status(result) mustBe OK
      (contentAsJson(result) \ "success").as[Boolean] mustBe true
      (contentAsJson(result) \ "accessToken")
        .asOpt[String]
        .isDefined mustBe true
    }

    "return 401 when authentication fails" in {
      val loginRequest = LoginRequestTest("wronguser", "wrongpass")

      when(mockAuthService.authenticate(any[String], any[String]))
        .thenReturn(Future.successful(None))

      val request = FakeRequest(POST, "/api/auth/login")
        .withHeaders("Csrf-Token" -> "nocheck")
        .withJsonBody(
          Json.toJson(loginRequest)(models.ModelFormatters.loginRequestFormat)
        )

      val result = route(app, request).get

      status(result) mustBe UNAUTHORIZED
      (contentAsJson(result) \ "success").as[Boolean] mustBe false
    }
  }

  "AuthController refreshToken" should {
    "return 200 and new tokens when refresh token is valid" in {
      val refreshRequest = RefreshTokenRequestTest("valid.refresh.token")
      val tokenPair =
        new service.TokenPair("new.access.token", "new.refresh.token")

      when(mockJwtService.refreshTokens(any[String]))
        .thenReturn(Future.successful(Some(tokenPair)))

      val request = FakeRequest(POST, "/api/auth/refresh")
        .withHeaders("Csrf-Token" -> "nocheck")
        .withJsonBody(
          Json.toJson(refreshRequest)(
            models.ModelFormatters.refreshTokenRequestFormat
          )
        )

      val result = route(app, request).get

      status(result) mustBe OK
      (contentAsJson(result) \ "success").as[Boolean] mustBe true
      (contentAsJson(result) \ "accessToken")
        .asOpt[String]
        .isDefined mustBe true
    }

    "return 401 when refresh token is invalid" in {
      val refreshRequest = RefreshTokenRequestTest("invalid.refresh.token")

      when(mockJwtService.refreshTokens(any[String]))
        .thenReturn(Future.successful(None))

      val request = FakeRequest(POST, "/api/auth/refresh")
        .withHeaders("Csrf-Token" -> "nocheck")
        .withJsonBody(
          Json.toJson(refreshRequest)(
            models.ModelFormatters.refreshTokenRequestFormat
          )
        )

      val result = route(app, request).get

      status(result) mustBe UNAUTHORIZED
      (contentAsJson(result) \ "success").as[Boolean] mustBe false
    }
  }

  "AuthController verifyToken" should {
    "return 200 when token is valid" in {
      val token = "Bearer valid.token"
      val userId = 1L
      val jwtClaim = service.JwtClaim(
        subject = userId.toString,
        username = "testuser",
        jwtId = Some("jwtid123"),
        tokenType = "access"
      )

      when(mockJwtService.verifyToken(any[String]))
        .thenReturn(Future.successful(Some(jwtClaim)))
      when(mockTokenBlacklistService.isBlacklisted(any[String]))
        .thenReturn(Future.successful(false))

      val request = FakeRequest(GET, "/api/auth/verify")
        .withHeaders(AUTHORIZATION -> token)

      val result = route(app, request).get

      status(result) mustBe OK
      (contentAsJson(result) \ "valid").as[Boolean] mustBe true
    }

    "return 401 when token is invalid" in {
      val token = "Bearer invalid.token"

      when(mockJwtService.verifyToken(any[String]))
        .thenReturn(Future.successful(None))

      val request = FakeRequest(GET, "/api/auth/verify")
        .withHeaders(AUTHORIZATION -> token)

      val result = route(app, request).get

      status(result) mustBe UNAUTHORIZED
    }
  }

  "AuthController logout" should {
    "return 200 when logout is successful" in {
      val token = "Bearer valid.token"

      when(mockJwtService.invalidateAccessToken(any[String]))
        .thenReturn(Future.successful(true))
      when(mockJwtService.invalidateRefreshToken(any[String]))
        .thenReturn(Future.successful(true))

      val request = FakeRequest(POST, "/api/auth/logout")
        .withHeaders(AUTHORIZATION -> token, "Csrf-Token" -> "nocheck")
        .withJsonBody(Json.obj("refreshToken" -> "refresh.token"))

      val result = route(app, request).get

      status(result) mustBe OK
      (contentAsJson(result) \ "success").as[Boolean] mustBe true
    }
  }
}
