package services

import java.time.ZonedDateTime
import java.util.Date

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import helpers.TestHelpers
import models.User
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Configuration
import play.api.test.Injecting
import service.{JwtClaim, JwtService, RedisService, TokenBlacklistService}

class JwtServiceSpec extends PlaySpec
    with GuiceOneAppPerSuite
    with MockitoSugar
    with BeforeAndAfterEach
    with ScalaFutures
    with Injecting
    with TestHelpers {

  val mockTokenBlacklistService: TokenBlacklistService = mock[TokenBlacklistService]
  val mockRedisService: RedisService = mock[RedisService]
  
  // Mock configuration
  val configData = Map(
    "jwt.secret" -> "test-secret-key-for-testing-purposes-only",
    "jwt.issuer" -> "test-issuer",
    "jwt.access.expiration" -> "3600",
    "jwt.refresh.expiration" -> "86400",
    "redis.refresh.prefix" -> "refresh:"
  )
  val config = Configuration.from(configData)
  
  // Create the service with mocked dependencies
  val jwtService = new JwtService(config, mockTokenBlacklistService, mockRedisService)
  
  // Test user
  val testUser = User(
    id = Some(1L),
    username = "testuser",
    email = "test@example.com",
    passwordHash = "hashed-password",
    createdAt = ZonedDateTime.now(),
    updatedAt = ZonedDateTime.now()
  )
  
  override def beforeEach(): Unit = {
    reset(mockTokenBlacklistService, mockRedisService)
  }
  
  "JwtService" should {
    "create valid access token" in {
      // When
      val token = jwtService.createAccessToken(testUser)
      
      // Then
      val decoded = JWT.decode(token)
      decoded.getSubject mustBe "1"
      decoded.getIssuer mustBe "test-issuer"
      decoded.getClaim("username").asString() mustBe "testuser"
      decoded.getClaim("tokenType").asString() mustBe "access"
    }
    
    "create valid refresh token" in {
      // Setup
      when(mockRedisService.setEx(anyString(), anyString(), anyLong()))
        .thenReturn(scala.util.Success(()))
      
      // When
      val tokenFuture = jwtService.createRefreshToken(testUser)
      val token = Await.result(tokenFuture, 5.seconds)
      
      // Then
      val decoded = JWT.decode(token)
      decoded.getSubject mustBe "1"
      decoded.getIssuer mustBe "test-issuer"
      decoded.getClaim("tokenType").asString() mustBe "refresh"
      
      // Verify Redis interaction
      verify(mockRedisService).setEx(
        org.mockito.ArgumentMatchers.matches("refresh:1:.*"),
        org.mockito.ArgumentMatchers.eq("1"),
        org.mockito.ArgumentMatchers.eq(86400L)
      )
    }
    
    "verify valid access token" in {
      // Setup
      when(mockTokenBlacklistService.isBlacklisted(any[String]))
        .thenReturn(Future.successful(false))
      
      // Create a token
      val token = jwtService.createAccessToken(testUser)
      
      // When
      val claimFuture = jwtService.verifyAccessToken(token)
      val claimOpt = Await.result(claimFuture, 5.seconds)
      
      // Then
      claimOpt must not be None
      claimOpt.get.subject mustBe "1"
      claimOpt.get.username mustBe "testuser"
      claimOpt.get.tokenType mustBe "access"
    }
    
    "reject blacklisted access token" in {
      // Setup
      val token = jwtService.createAccessToken(testUser)
      when(mockTokenBlacklistService.isBlacklisted(token))
        .thenReturn(Future.successful(true))
      
      // When
      val claimFuture = jwtService.verifyAccessToken(token)
      val claimOpt = Await.result(claimFuture, 5.seconds)
      
      // Then
      claimOpt mustBe None
    }
    
    "verify valid refresh token" in {
      // Setup
      when(mockRedisService.setEx(anyString(), anyString(), anyLong()))
        .thenReturn(scala.util.Success(()))
      
      // Create a refresh token
      val tokenFuture = jwtService.createRefreshToken(testUser)
      val token = Await.result(tokenFuture, 5.seconds)
      
      // Mock the Redis get operation to return the user ID
      when(mockRedisService.get(org.mockito.ArgumentMatchers.matches("refresh:1:.*")))
        .thenReturn(scala.util.Success(Some("1")))
      
      // When
      val userIdFuture = jwtService.verifyRefreshToken(token)
      val userIdOpt = Await.result(userIdFuture, 5.seconds)
      
      // Then
      userIdOpt must not be None
      userIdOpt.get mustBe "1"
    }
    
    "reject refresh token not in Redis" in {
      // Setup
      when(mockRedisService.setEx(anyString(), anyString(), anyLong()))
        .thenReturn(scala.util.Success(()))
      
      // Create a refresh token
      val tokenFuture = jwtService.createRefreshToken(testUser)
      val token = Await.result(tokenFuture, 5.seconds)
      
      // Mock the Redis get operation to return None (token not found)
      when(mockRedisService.get(org.mockito.ArgumentMatchers.matches("refresh:1:.*")))
        .thenReturn(scala.util.Success(None))
      
      // When
      val userIdFuture = jwtService.verifyRefreshToken(token)
      val userIdOpt = Await.result(userIdFuture, 5.seconds)
      
      // Then
      userIdOpt mustBe None
    }
    
    "refresh tokens successfully with valid refresh token" in {
      // Setup
      when(mockRedisService.setEx(anyString(), anyString(), anyLong()))
        .thenReturn(scala.util.Success(()))
      
      // Create a refresh token
      val tokenFuture = jwtService.createRefreshToken(testUser)
      val refreshToken = Await.result(tokenFuture, 5.seconds)
      
      // Mock the Redis operations
      when(mockRedisService.get(org.mockito.ArgumentMatchers.matches("refresh:1:.*")))
        .thenReturn(scala.util.Success(Some("1")))
      when(mockRedisService.delete(anyString()))
        .thenReturn(scala.util.Success(1L))
      
      // When
      val tokenPairFuture = jwtService.refreshTokens(refreshToken)
      val tokenPairOpt = Await.result(tokenPairFuture, 5.seconds)
      
      // Then
      tokenPairOpt must not be None
      tokenPairOpt.get.accessToken must not be empty
      tokenPairOpt.get.refreshToken must not be empty
      
      // Verify old token was invalidated
      verify(mockRedisService).delete(anyString())
    }
    
    "invalidate access token" in {
      // Setup
      val token = jwtService.createAccessToken(testUser)
      when(mockTokenBlacklistService.blacklistToken(token))
        .thenReturn(Future.successful(true))
      
      // When
      val resultFuture = jwtService.invalidateAccessToken(token)
      val result = Await.result(resultFuture, 5.seconds)
      
      // Then
      result mustBe true
      verify(mockTokenBlacklistService).blacklistToken(token)
    }
    
    "invalidate refresh token" in {
      // Setup
      when(mockRedisService.setEx(anyString(), anyString(), anyLong()))
        .thenReturn(scala.util.Success(()))
      
      // Create a refresh token
      val tokenFuture = jwtService.createRefreshToken(testUser)
      val token = Await.result(tokenFuture, 5.seconds)
      
      // Mock the delete operation
      when(mockRedisService.delete(anyString()))
        .thenReturn(scala.util.Success(1L))
      
      // When
      val resultFuture = jwtService.invalidateRefreshToken(token)
      val result = Await.result(resultFuture, 5.seconds)
      
      // Then
      result mustBe true
      verify(mockRedisService).delete(org.mockito.ArgumentMatchers.matches("refresh:1:.*"))
    }
  }
}