package controllers

import java.time.ZonedDateTime

import scala.concurrent.Future

import helpers.TestWithDBCleaner
import models.{RegistrationRequestTest, User, UserRepository}
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
import service.{AuthService, JwtService}

class UserControllerSpec
    extends TestWithDBCleaner
    with GuiceOneAppPerSuite
    with MockitoSugar
    with BeforeAndAfterEach {

  val mockUserRepository: UserRepository = mock[UserRepository]
  val mockAuthService: AuthService = mock[AuthService]
  val mockJwtService: JwtService = mock[JwtService]

  override def fakeApplication(): Application = {
    GuiceApplicationBuilder()
      .overrides(
        bind[UserRepository].toInstance(mockUserRepository),
        bind[AuthService].toInstance(mockAuthService),
        bind[JwtService].toInstance(mockJwtService),
        bind[services.MockModule].toInstance(new services.MockModule)
      )
      .build()
  }

  override def beforeEach(): Unit = {
    reset(mockUserRepository, mockAuthService, mockJwtService)
  }

  "UserController register" should {
    "return 201 when user registration is successful" in {
      val registerRequest =
        RegistrationRequestTest("newuser", "user@example.com", "password@123")
      val now = ZonedDateTime.now()
      val newUser =
        User(Some(1L), "newuser", "user@example.com", now, now)

      when(mockAuthService.register(any[String], any[String], any[String]))
        .thenReturn(Future.successful(Right(newUser)))

      val request = FakeRequest(POST, "/api/auth/register")
        .withHeaders("Csrf-Token" -> "nocheck")
        .withJsonBody(
          Json.toJson(registerRequest)(using
            models.ModelFormatters.registrationRequestFormat
          )
        )

      val result = route(app, request).get

      status(result) mustBe CREATED
      (contentAsJson(result) \ "success").as[Boolean] mustBe true
    }

    "return 409 when username is already taken" in {
      val registerRequest = RegistrationRequestTest(
        "existinguser",
        "user@example.com",
        "password@123"
      )

      when(mockAuthService.register(any[String], any[String], any[String]))
        .thenReturn(Future.successful(Left("Username already exists")))

      val request = FakeRequest(POST, "/api/auth/register")
        .withHeaders("Csrf-Token" -> "nocheck")
        .withJsonBody(
          Json.toJson(registerRequest)(using
            models.ModelFormatters.registrationRequestFormat
          )
        )

      val result = route(app, request).get

      status(result) mustBe CONFLICT
      (contentAsJson(result) \ "success").as[Boolean] mustBe false
    }

    "return 409 when email is already taken" in {
      val registerRequest = RegistrationRequestTest(
        "newuser",
        "existing@example.com",
        "password@123"
      )

      when(mockAuthService.register(any[String], any[String], any[String]))
        .thenReturn(Future.successful(Left("Email already exists")))

      val request = FakeRequest(POST, "/api/auth/register")
        .withHeaders("Csrf-Token" -> "nocheck")
        .withJsonBody(
          Json.toJson(registerRequest)(using
            models.ModelFormatters.registrationRequestFormat
          )
        )

      val result = route(app, request).get

      status(result) mustBe CONFLICT
      (contentAsJson(result) \ "success").as[Boolean] mustBe false
    }
  }

  "UserController getUser" should {
    "return 200 with user data when user exists" in {
      val now = ZonedDateTime.now()
      val user =
        User(Some(1L), "testuser", "test@example.com", now, now)

      when(mockUserRepository.getById(any[Long]))
        .thenReturn(Future.successful(Some(user)))

      val request = FakeRequest(GET, "/api/users/1")
      val result = route(app, request).get

      status(result) mustBe OK
      (contentAsJson(result) \ "username").as[String] mustBe "testuser"
      // Should not contain sensitive data
      (contentAsJson(result) \ "passwordHash").isEmpty mustBe true
    }

    "return 404 when user does not exist" in {
      when(mockUserRepository.getById(any[Long]))
        .thenReturn(Future.successful(None))

      val request = FakeRequest(GET, "/api/users/999")
      val result = route(app, request).get

      status(result) mustBe NOT_FOUND
    }
  }
}
