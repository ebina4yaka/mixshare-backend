package services

import javax.inject.{Inject, Singleton}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import com.google.inject.{AbstractModule, Provides}
import models.{UserPassword, UserPasswordRepository}
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import play.api.Configuration
import service.{RedisService, TokenBlacklistService}

class MockModule extends AbstractModule with MockitoSugar {
  @Provides
  @Singleton
  def provideMockRedisService(configuration: Configuration): RedisService = {
    new MockRedisService(configuration)
  }

  @Provides
  @Singleton
  def provideMockUserPasswordRepository(): UserPasswordRepository = {
    val mockRepo = mock[UserPasswordRepository]
    // Setup common mock behavior if needed
    mockRepo
  }

  override def configure(): Unit = {
    // No manual binding needed, using @Provides
  }
}
