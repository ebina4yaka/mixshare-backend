package services

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.{Success, Try}

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import helpers.TestHelpers
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Configuration
import play.api.test.Injecting
import service.{RedisService, TokenBlacklistService}

class TokenBlacklistServiceSpec
    extends PlaySpec
    with GuiceOneAppPerSuite
    with MockitoSugar
    with BeforeAndAfterEach
    with ScalaFutures
    with Injecting
    with TestHelpers {

  val mockRedisService: RedisService = mock[RedisService]

  // Mock configuration
  val configData = Map(
    "redis.blacklist.prefix" -> "blacklist:",
    "jwt.secret" -> "test-secret-key-for-testing-purposes-only",
    "jwt.issuer" -> "test-issuer"
  )
  val config = Configuration.from(configData)

  // Create the service with mocked dependencies
  val tokenBlacklistService =
    new TokenBlacklistService(mockRedisService, config)

  // Test JWT
  val algorithm = Algorithm.HMAC256(configData("jwt.secret"))
  val testToken = JWT
    .create()
    .withIssuer(configData("jwt.issuer"))
    .withSubject("1")
    .withClaim("username", "testuser")
    .withClaim("tokenType", "access")
    .withIssuedAt(new java.util.Date())
    .withExpiresAt(
      new java.util.Date(System.currentTimeMillis() + 3600 * 1000)
    ) // 1 hour
    .withJWTId("test-token-id")
    .sign(algorithm)

  val tokenWithoutJti = JWT
    .create()
    .withIssuer(configData("jwt.issuer"))
    .withSubject("1")
    .withClaim("username", "testuser")
    .withClaim("tokenType", "access")
    .withIssuedAt(new java.util.Date())
    .withExpiresAt(
      new java.util.Date(System.currentTimeMillis() + 3600 * 1000)
    ) // 1 hour
    .sign(algorithm)

  override def beforeEach(): Unit = {
    reset(mockRedisService)
  }

  "TokenBlacklistService" should {
    "blacklist token with JTI" in {
      // Setup
      when(mockRedisService.setEx(anyString(), anyString(), anyLong()))
        .thenReturn(Success(()))

      // When
      val resultFuture = tokenBlacklistService.blacklistToken(testToken)
      val result = Await.result(resultFuture, 5.seconds)

      // Then
      result mustBe true

      // Verify Redis interaction with correct key
      verify(mockRedisService).setEx(
        org.mockito.ArgumentMatchers.eq("blacklist:test-token-id"),
        org.mockito.ArgumentMatchers.eq("1"),
        anyLong()
      )
    }

    "blacklist token without JTI" in {
      // Setup
      when(mockRedisService.setEx(anyString(), anyString(), anyLong()))
        .thenReturn(Success(()))

      // When
      val resultFuture = tokenBlacklistService.blacklistToken(tokenWithoutJti)
      val result = Await.result(resultFuture, 5.seconds)

      // Then
      result mustBe true

      // Verify Redis interaction with hash-based key
      verify(mockRedisService).setEx(
        org.mockito.ArgumentMatchers.matches("blacklist:.*"),
        org.mockito.ArgumentMatchers.eq("1"),
        anyLong()
      )
    }

    "handle Redis failure when blacklisting" in {
      // Setup
      when(mockRedisService.setEx(anyString(), anyString(), anyLong()))
        .thenReturn(
          scala.util.Failure(new RuntimeException("Redis connection failed"))
        )

      // When
      val resultFuture = tokenBlacklistService.blacklistToken(testToken)
      val result = Await.result(resultFuture, 5.seconds)

      // Then
      result mustBe false
    }

    "detect blacklisted token" in {
      // Setup
      when(mockRedisService.exists(anyString()))
        .thenReturn(Success(true))

      // When
      val resultFuture = tokenBlacklistService.isBlacklisted(testToken)
      val result = Await.result(resultFuture, 5.seconds)

      // Then
      result mustBe true

      // Verify Redis interaction with correct key
      verify(mockRedisService).exists("blacklist:test-token-id")
    }

    "detect non-blacklisted token" in {
      // Setup
      when(mockRedisService.exists(anyString()))
        .thenReturn(Success(false))

      // When
      val resultFuture = tokenBlacklistService.isBlacklisted(testToken)
      val result = Await.result(resultFuture, 5.seconds)

      // Then
      result mustBe false
    }

    "handle Redis failure when checking blacklist" in {
      // Setup
      when(mockRedisService.exists(anyString()))
        .thenReturn(
          scala.util.Failure(new RuntimeException("Redis connection failed"))
        )

      // When
      val resultFuture = tokenBlacklistService.isBlacklisted(testToken)
      val result = Await.result(resultFuture, 5.seconds)

      // Then
      result mustBe false
    }
  }
}
