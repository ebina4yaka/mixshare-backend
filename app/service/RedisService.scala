package service

import javax.inject.{Inject, Singleton}

import scala.util.{Failure, Success, Try}

import play.api.{Configuration, Logger}
import redis.clients.jedis.{Jedis, JedisPool, JedisPoolConfig}

/** Service for Redis operations. Provides a connection pool and methods for
  * common Redis operations.
  */
@Singleton
class RedisService @Inject() (config: Configuration) {

  private val logger = Logger(this.getClass)

  // Redis configuration
  private val redisHost = config.get[String]("redis.host")
  private val redisPort = config.get[Int]("redis.port")
  private val redisDatabase = config.get[Int]("redis.database")
  private val redisTimeout = config.get[Int]("redis.timeout")
  private val redisPassword = config.getOptional[String]("redis.password")

  // Configure connection pool
  private val poolConfig = new JedisPoolConfig()
  poolConfig.setMaxTotal(10)
  poolConfig.setMaxIdle(5)
  poolConfig.setMinIdle(1)
  poolConfig.setTestOnBorrow(true)
  poolConfig.setTestOnReturn(true)
  poolConfig.setTestWhileIdle(true)

  // Initialize connection pool
  private val jedisPool: JedisPool = redisPassword match {
    case Some(password) if password.nonEmpty =>
      new JedisPool(poolConfig, redisHost, redisPort, redisTimeout, password)
    case _ =>
      new JedisPool(poolConfig, redisHost, redisPort, redisTimeout)
  }

  logger.info(
    s"Redis connection pool initialized: $redisHost:$redisPort/db:$redisDatabase"
  )

  /** Execute a Redis operation with resource management.
    *
    * @param operation
    *   The operation to execute on the Redis connection
    * @return
    *   The result of the operation wrapped in a Try
    */
  def withRedis[T](operation: Jedis => T): Try[T] = {
    var jedis: Jedis = null
    try {
      jedis = jedisPool.getResource
      jedis.select(redisDatabase)
      Success(operation(jedis))
    } catch {
      case e: Exception =>
        logger.error(s"Redis operation failed: ${e.getMessage}", e)
        Failure(e)
    } finally {
      if (jedis != null) {
        jedis.close()
      }
    }
  }

  /** Set a key with expiration.
    *
    * @param key
    *   The key to set
    * @param value
    *   The value to set
    * @param expireSeconds
    *   Expiration time in seconds
    * @return
    *   Success if the operation succeeded, Failure otherwise
    */
  def setEx(key: String, value: String, expireSeconds: Long): Try[String] = {
    withRedis { jedis =>
      jedis.setex(key, expireSeconds, value)
    }
  }

  /** Get a key's value.
    *
    * @param key
    *   The key to get
    * @return
    *   Success with the value if found, Failure otherwise
    */
  def get(key: String): Try[Option[String]] = {
    withRedis { jedis =>
      Option(jedis.get(key))
    }
  }

  /** Check if a key exists.
    *
    * @param key
    *   The key to check
    * @return
    *   Success with true if the key exists, false otherwise
    */
  def exists(key: String): Try[Boolean] = {
    withRedis { jedis =>
      jedis.exists(key)
    }
  }

  /** Delete a key.
    *
    * @param key
    *   The key to delete
    * @return
    *   Success with the number of keys removed
    */
  def delete(key: String): Try[Long] = {
    withRedis { jedis =>
      jedis.del(key)
    }
  }

  /** Shutdown the Redis connection pool.
    */
  def shutdown(): Unit = {
    jedisPool.close()
    logger.info("Redis connection pool closed")
  }
}
