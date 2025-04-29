package service

import java.security.MessageDigest
import java.time.ZonedDateTime
import java.util.Base64
import javax.inject.{Inject, Singleton}

import scala.concurrent.{ExecutionContext, Future}

import models.{User, UserRepository}

@Singleton
class AuthService @Inject() (
    userRepository: UserRepository
)(implicit ec: ExecutionContext) {

  // Simple password hashing function
  def hashPassword(password: String): String = {
    val md = MessageDigest.getInstance("SHA-256")
    val hash = md.digest(password.getBytes("UTF-8"))
    Base64.getEncoder.encodeToString(hash)
  }

  // Authenticate a user by username/email and password
  def authenticate(username: String, password: String): Future[Option[User]] = {
    // Search for user by username
    // In a real application, you might want to add email lookup too
    val query = for {
      users <- userRepository.findByUsername(username)
    } yield users

    query.map {
      case Some(user) if user.passwordHash == hashPassword(password) =>
        Some(user)
      case _ => None
    }
  }

  // Register a new user
  def register(
      username: String,
      email: String,
      password: String
  ): Future[Either[String, User]] = {
    // Check if username or email already exists
    val checkExisting = for {
      existingUsername <- userRepository.findByUsername(username)
      existingEmail <- userRepository.findByEmail(email)
    } yield (existingUsername, existingEmail)

    checkExisting.flatMap {
      case (Some(_), _) => Future.successful(Left("Username already exists"))
      case (_, Some(_)) => Future.successful(Left("Email already exists"))
      case (None, None) =>
        // Create new user with hashed password
        val newUser = User(
          id = None,
          username = username,
          email = email,
          passwordHash = hashPassword(password),
          createdAt = ZonedDateTime.now(),
          updatedAt = ZonedDateTime.now()
        )

        userRepository.create(newUser).map(user => Right(user))
    }
  }
}
