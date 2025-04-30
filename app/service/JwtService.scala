package service

import java.util.{Date, UUID}
import javax.inject.{Inject, Singleton}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import models.User
import play.api.{Configuration, Logger}

@Singleton
class JwtService @Inject() (
    config: Configuration,
    tokenBlacklistService: TokenBlacklistService,
    redisService: RedisService
)(implicit ec: ExecutionContext) {

  private val logger = Logger(this.getClass)

  // JWT Configuration
  private val secret: String = config.get[String]("jwt.secret")
  private val accessTokenExpirationSeconds: Long =
    config.get[Long]("jwt.access.expiration")
  private val refreshTokenExpirationSeconds: Long =
    config.get[Long]("jwt.refresh.expiration")
  private val issuer: String = config.get[String]("jwt.issuer")

  // Redis key prefix for refresh tokens
  private val refreshTokenPrefix = config.get[String]("redis.refresh.prefix")

  // Algorithm used for JWT signing
  private val algorithm = Algorithm.HMAC256(secret)

  /** Create an access token for a user
    *
    * @param user
    *   The user to create a token for
    * @return
    *   The JWT access token
    */
  def createAccessToken(user: User): String = {
    val now = new Date()
    val expiryDate = new Date(now.getTime + accessTokenExpirationSeconds * 1000)
    val tokenId = UUID.randomUUID().toString

    JWT
      .create()
      .withIssuer(issuer)
      .withSubject(user.id.getOrElse(0L).toString)
      .withClaim("username", user.username)
      .withClaim("tokenType", "access")
      .withIssuedAt(now)
      .withExpiresAt(expiryDate)
      .withJWTId(tokenId)
      .sign(algorithm)
  }

  /** Create a refresh token for a user
    *
    * @param user
    *   The user to create a token for
    * @return
    *   The JWT refresh token
    */
  def createRefreshToken(user: User): Future[String] = Future {
    val now = new Date()
    val expiryDate =
      new Date(now.getTime + refreshTokenExpirationSeconds * 1000)
    val tokenId = UUID.randomUUID().toString

    val refreshToken = JWT
      .create()
      .withIssuer(issuer)
      .withSubject(user.id.getOrElse(0L).toString)
      .withClaim("tokenType", "refresh")
      .withIssuedAt(now)
      .withExpiresAt(expiryDate)
      .withJWTId(tokenId)
      .sign(algorithm)

    // Store the refresh token in Redis
    val userId = user.id.getOrElse(0L).toString
    val redisKey = s"$refreshTokenPrefix$userId:$tokenId"

    try {
      redisService.setEx(redisKey, userId, refreshTokenExpirationSeconds)
      logger.info(s"Refresh token stored for user $userId with ID $tokenId")
    } catch {
      case e: Exception =>
        logger.error(s"Failed to store refresh token: ${e.getMessage}")
    }

    refreshToken
  }

  /** Verify and decode access token
    *
    * @param token
    *   The JWT token to verify
    * @return
    *   Future[Option[JwtClaim]] with token claims if valid
    */
  def verifyAccessToken(token: String): Future[Option[JwtClaim]] = {
    // First check if token is blacklisted
    tokenBlacklistService.isBlacklisted(token).flatMap { isBlacklisted =>
      if (isBlacklisted) {
        Future.successful(None) // Token is blacklisted
      } else {
        Future.successful {
          try {
            val verifier = JWT
              .require(algorithm)
              .withIssuer(issuer)
              .withClaim("tokenType", "access")
              .build()

            val decodedJWT = verifier.verify(token)

            Some(
              JwtClaim(
                subject = decodedJWT.getSubject,
                username = decodedJWT.getClaim("username").asString(),
                jwtId = Option(decodedJWT.getId()),
                tokenType = "access"
              )
            )
          } catch {
            case e: JWTVerificationException =>
              logger.debug(s"Access token verification failed: ${e.getMessage}")
              None
          }
        }
      }
    }
  }

  /** Verify a refresh token and return user ID if valid
    *
    * @param token
    *   The refresh token to verify
    * @return
    *   Future[Option[String]] with user ID if valid
    */
  def verifyRefreshToken(token: String): Future[Option[String]] = Future {
    try {
      val verifier = JWT
        .require(algorithm)
        .withIssuer(issuer)
        .withClaim("tokenType", "refresh")
        .build()

      val decodedJWT = verifier.verify(token)
      val userId = decodedJWT.getSubject
      val tokenId = decodedJWT.getId()

      // Check if token exists in Redis
      val redisKey = s"$refreshTokenPrefix$userId:$tokenId"

      redisService.get(redisKey) match {
        case scala.util.Success(Some(storedUserId)) if storedUserId == userId =>
          // Token is valid and stored in Redis
          Some(userId)
        case _ =>
          // Token not found in Redis or doesn't match
          None
      }
    } catch {
      case e: Exception =>
        logger.debug(s"Refresh token verification failed: ${e.getMessage}")
        None
    }
  }

  /** Refresh the access token using a valid refresh token
    *
    * @param refreshToken
    *   The refresh token
    * @return
    *   Future[Option[TokenPair]] with new access and refresh tokens if
    *   successful
    */
  def refreshTokens(refreshToken: String): Future[Option[TokenPair]] = {
    verifyRefreshToken(refreshToken).flatMap {
      case Some(userId) =>
        // Find the user
        Try(userId.toLong).toOption match {
          case Some(id) =>
            // Get user from repository
            // For simplicity, we'll create a minimal User object here
            val user = User(
              id = Some(id.toLong),
              username = "", // We don't need username for token generation
              email = "",
              createdAt =
                new Date().toInstant.atZone(java.time.ZoneId.systemDefault),
              updatedAt =
                new Date().toInstant.atZone(java.time.ZoneId.systemDefault)
            )

            // Invalidate the old refresh token
            invalidateRefreshToken(refreshToken)

            // Generate new tokens
            for {
              newRefreshToken <- createRefreshToken(user)
              newAccessToken = createAccessToken(user)
            } yield Some(TokenPair(newAccessToken, newRefreshToken))

          case None =>
            Future.successful(None)
        }
      case None =>
        Future.successful(None)
    }
  }

  /** Backwards compatibility method for existing code
    */
  def createToken(user: User): String = {
    createAccessToken(user)
  }

  /** Backwards compatibility method for existing code
    */
  def verifyToken(token: String): Future[Option[JwtClaim]] = {
    verifyAccessToken(token)
  }

  /** Invalidate an access token
    *
    * @param token
    *   The access token to invalidate
    * @return
    *   Future[Boolean] indicating success
    */
  def invalidateAccessToken(token: String): Future[Boolean] = {
    tokenBlacklistService.blacklistToken(token)
  }

  /** Backwards compatibility method for existing code
    */
  def invalidateToken(token: String): Future[Boolean] = {
    invalidateAccessToken(token)
  }

  /** Invalidate a refresh token
    *
    * @param token
    *   The refresh token to invalidate
    * @return
    *   Future[Boolean] indicating success
    */
  def invalidateRefreshToken(token: String): Future[Boolean] = Future {
    try {
      val jwt = JWT.decode(token)
      val userId = jwt.getSubject
      val tokenId = jwt.getId()

      // Delete from Redis
      val redisKey = s"$refreshTokenPrefix$userId:$tokenId"
      val result = redisService.delete(redisKey)

      result.isSuccess
    } catch {
      case e: Exception =>
        logger.error(s"Failed to invalidate refresh token: ${e.getMessage}")
        false
    }
  }

  /** Invalidate all refresh tokens for a user
    *
    * @param userId
    *   The user ID
    * @return
    *   Future[Boolean] indicating success
    */
  def invalidateAllUserRefreshTokens(userId: Long): Future[Boolean] = Future {
    // This would require a scan pattern in Redis
    // For simplicity, we're not implementing this fully
    logger.info(
      s"Invalidation of all refresh tokens for user $userId would be implemented here"
    )
    true
  }
}

// JWT Claim model
case class JwtClaim(
    subject: String, // Contains the user ID
    username: String,
    jwtId: Option[String] = None, // JWT ID for invalidation tracking
    tokenType: String = "access" // Type of token (access or refresh)
)

// Token pair for access and refresh tokens
case class TokenPair(
    accessToken: String,
    refreshToken: String
)
