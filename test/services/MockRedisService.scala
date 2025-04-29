package services

import scala.util.{Success, Try}

import play.api.Configuration
import service.RedisService

class MockRedisService(config: Configuration) extends RedisService(config) {
  private var cache = Map[String, String]()

  override def setEx(
      key: String,
      value: String,
      expireSeconds: Long
  ): Try[String] = {
    cache += (key -> value)
    Success("OK")
  }

  override def get(key: String): Try[Option[String]] = {
    Success(cache.get(key))
  }

  override def delete(key: String): Try[Long] = {
    val existed = cache.contains(key)
    cache -= key
    Success(if (existed) 1L else 0L)
  }

  override def exists(key: String): Try[Boolean] = {
    Success(cache.contains(key))
  }
}
