package services

import java.time.ZonedDateTime

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

import helpers.TestHelpers
import models.{User, UserRepository}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.test.Injecting
import service.AuthService

class AuthServiceSpec
    extends PlaySpec
    with GuiceOneAppPerSuite
    with MockitoSugar
    with BeforeAndAfterEach
    with ScalaFutures
    with Injecting
    with TestHelpers {

  val mockUserRepository: UserRepository = mock[UserRepository]

  // Create the service with mocked dependencies
  val authService = new AuthService(mockUserRepository)

  // Test data
  val testUser = User(
    id = Some(1L),
    username = "testuser",
    email = "test@example.com",
    passwordHash = authService.hashPassword("password123"),
    createdAt = ZonedDateTime.now(),
    updatedAt = ZonedDateTime.now()
  )

  override def beforeEach(): Unit = {
    reset(mockUserRepository)
  }

  "AuthService" should {
    "hash password correctly" in {
      // When
      val hash1 = authService.hashPassword("password123")
      val hash2 = authService.hashPassword("password123")
      val hash3 = authService.hashPassword("different")

      // Then
      hash1 mustBe hash2 // Same password should produce the same hash
      hash1 must not be hash3 // Different passwords should produce different hashes
    }

    "authenticate user with correct credentials" in {
      // Setup
      when(mockUserRepository.findByUsername("testuser"))
        .thenReturn(Future.successful(Some(testUser)))

      // When
      val userFuture = authService.authenticate("testuser", "password123")
      val userOpt = Await.result(userFuture, 5.seconds)

      // Then
      userOpt must not be None
      userOpt.get.id mustBe Some(1L)
      userOpt.get.username mustBe "testuser"
    }

    "reject authentication with incorrect password" in {
      // Setup
      when(mockUserRepository.findByUsername("testuser"))
        .thenReturn(Future.successful(Some(testUser)))

      // When
      val userFuture = authService.authenticate("testuser", "wrongpassword")
      val userOpt = Await.result(userFuture, 5.seconds)

      // Then
      userOpt mustBe None
    }

    "reject authentication with non-existent username" in {
      // Setup
      when(mockUserRepository.findByUsername("nonexistent"))
        .thenReturn(Future.successful(None))

      // When
      val userFuture = authService.authenticate("nonexistent", "password123")
      val userOpt = Await.result(userFuture, 5.seconds)

      // Then
      userOpt mustBe None
    }

    "register a new user successfully" in {
      // Setup
      when(mockUserRepository.findByUsername("newuser"))
        .thenReturn(Future.successful(None))
      when(mockUserRepository.findByEmail("new@example.com"))
        .thenReturn(Future.successful(None))

      val newUser = User(
        id = Some(2L),
        username = "newuser",
        email = "new@example.com",
        passwordHash = authService.hashPassword("newpassword"),
        createdAt = ZonedDateTime.now(),
        updatedAt = ZonedDateTime.now()
      )

      when(mockUserRepository.create(any[User]))
        .thenReturn(Future.successful(newUser))

      // When
      val resultFuture =
        authService.register("newuser", "new@example.com", "newpassword")
      val result = Await.result(resultFuture, 5.seconds)

      // Then
      result.isRight mustBe true
      result
        .getOrElse(fail("Expected Right but got Left"))
        .username mustBe "newuser"
      result
        .getOrElse(fail("Expected Right but got Left"))
        .email mustBe "new@example.com"

      // Verify the password was hashed
      verify(mockUserRepository).create(argThat { (user: User) =>
        user.passwordHash == authService.hashPassword("newpassword")
      })
    }

    "reject registration with existing username" in {
      // Setup
      when(mockUserRepository.findByUsername("testuser"))
        .thenReturn(Future.successful(Some(testUser)))
      when(mockUserRepository.findByEmail(any[String]))
        .thenReturn(Future.successful(None))

      // When
      val resultFuture =
        authService.register("testuser", "new@example.com", "newpassword")
      val result = Await.result(resultFuture, 5.seconds)

      // Then
      result.isLeft mustBe true
      result.swap.toOption.get mustBe "Username already exists"

      // Verify create was not called
      verify(mockUserRepository, never()).create(any[User])
    }

    "reject registration with existing email" in {
      // Setup
      when(mockUserRepository.findByUsername("newuser"))
        .thenReturn(Future.successful(None))
      when(mockUserRepository.findByEmail("test@example.com"))
        .thenReturn(Future.successful(Some(testUser)))

      // When
      val resultFuture =
        authService.register("newuser", "test@example.com", "newpassword")
      val result = Await.result(resultFuture, 5.seconds)

      // Then
      result.isLeft mustBe true
      result.swap.toOption.get mustBe "Email already exists"

      // Verify create was not called
      verify(mockUserRepository, never()).create(any[User])
    }
  }
}
