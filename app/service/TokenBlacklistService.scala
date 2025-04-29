package service

import java.time.Instant
import java.util.Date
import javax.inject.{Inject, Singleton}

import scala.concurrent.{ExecutionContext, Future}

import com.auth0.jwt.JWT
import play.api.{Configuration, Logger}

/** Service to manage blacklisted tokens using Redis.
  *
  * This implements a distributed token blacklist suitable for production use.
  */
@Singleton
class TokenBlacklistService @Inject() (
    redisService: RedisService,
    config: Configuration
)(implicit ec: ExecutionContext) {

  private val logger = Logger(this.getClass)

  // Get prefix for Redis keys
  private val keyPrefix = config.get[String]("redis.blacklist.prefix")

  /** Add a token to the blacklist
    *
    * @param token
    *   The JWT token to blacklist
    * @return
    *   Future[Boolean] indicating if the token was added to the blacklist
    */
  def blacklistToken(token: String): Future[Boolean] = Future {
    try {
      // Parse the token to get its expiration time
      val jwt = JWT.decode(token)
      val tokenId = jwt.getId() match {
        case null =>
          token.hashCode.toString // If no JTI claim, use hash of token
        case jti => jti
      }

      // Calculate time until expiration
      val expiryTime = Option(jwt.getExpiresAt) match {
        case Some(date) =>
          val now = Instant.now.toEpochMilli
          val expiresAt = date.getTime
          val ttlSeconds =
            math.max(1, (expiresAt - now) / 1000) // At least 1 second
          ttlSeconds
        case None =>
          86400L // Default to 24 hours if no exp claim
      }

      // Add to Redis blacklist with expiration (key: prefix+tokenId, value: "1")
      val redisKey = s"$keyPrefix$tokenId"
      val result = redisService.setEx(redisKey, "1", expiryTime)

      result match {
        case scala.util.Success(_) =>
          logger.info(
            s"Token blacklisted in Redis: $tokenId, expires in: $expiryTime seconds"
          )
          true
        case scala.util.Failure(e) =>
          logger.error(s"Failed to blacklist token in Redis: ${e.getMessage}")
          false
      }
    } catch {
      case e: Exception =>
        logger.error(s"Failed to blacklist token: ${e.getMessage}")
        false
    }
  }

  /** Check if a token is blacklisted
    *
    * @param token
    *   The JWT token to check
    * @return
    *   Future[Boolean] indicating if the token is blacklisted
    */
  def isBlacklisted(token: String): Future[Boolean] = Future {
    try {
      val jwt = JWT.decode(token)
      val tokenId = jwt.getId() match {
        case null => token.hashCode.toString
        case jti  => jti
      }

      // Check if token is in Redis blacklist
      val redisKey = s"$keyPrefix$tokenId"
      val result = redisService.exists(redisKey)

      result match {
        case scala.util.Success(exists) => exists
        case scala.util.Failure(e) =>
          logger.error(
            s"Failed to check if token is blacklisted in Redis: ${e.getMessage}"
          )
          false
      }
    } catch {
      case e: Exception =>
        logger.error(
          s"Failed to check if token is blacklisted: ${e.getMessage}"
        )
        false
    }
  }
}
