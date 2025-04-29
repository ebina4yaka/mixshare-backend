package services

import javax.inject.{Inject, Singleton}

import scala.concurrent.ExecutionContext.Implicits.global

import com.google.inject.{AbstractModule, Provides}
import play.api.Configuration
import service.{RedisService, TokenBlacklistService}

class MockModule extends AbstractModule {
  @Provides
  @Singleton
  def provideMockRedisService(configuration: Configuration): RedisService = {
    new MockRedisService(configuration)
  }

  override def configure(): Unit = {
    // No manual binding needed, using @Provides
  }
}
