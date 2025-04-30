package service

import java.security.MessageDigest
import java.time.ZonedDateTime
import java.util.Base64
import javax.inject.{Inject, Singleton}

import scala.concurrent.{ExecutionContext, Future}

import models.{User, UserPassword, UserPasswordRepository, UserRepository}

@Singleton
class AuthService @Inject() (
    userRepository: UserRepository,
    userPasswordRepository: UserPasswordRepository
)(implicit ec: ExecutionContext) {

  // Simple password hashing function
  def hashPassword(password: String): String = {
    val md = MessageDigest.getInstance("SHA-256")
    val hash = md.digest(password.getBytes("UTF-8"))
    Base64.getEncoder.encodeToString(hash)
  }

  // Authenticate a user by username/email and password
  def authenticate(username: String, password: String): Future[Option[User]] = {
    // First get the user
    userRepository.findByUsername(username).flatMap {
      case None       => Future.successful(None) // User not found
      case Some(user) =>
        // Then check the password
        user.id match {
          case None =>
            Future.successful(None) // User ID not set, should not happen
          case Some(userId) =>
            userPasswordRepository.findByUserId(userId).map {
              case Some(userPassword)
                  if userPassword.passwordHash == hashPassword(password) =>
                Some(user)
              case _ => None
            }
        }
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
        // Create new user
        val newUser = User(
          id = None,
          username = username,
          email = email,
          createdAt = ZonedDateTime.now(),
          updatedAt = ZonedDateTime.now()
        )

        // Save user and then save password
        userRepository.create(newUser).flatMap { user =>
          val userPassword = UserPassword(
            userId = user.id.get,
            passwordHash = hashPassword(password),
            createdAt = ZonedDateTime.now(),
            updatedAt = ZonedDateTime.now()
          )
          userPasswordRepository.create(userPassword).map(_ => Right(user))
        }
    }
  }
}
