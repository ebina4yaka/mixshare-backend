package actions

import javax.inject.Inject

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

import models.UserRepository
import play.api.mvc._
import service.JwtService

// Request with authentication information
case class AuthRequest[A](userId: Long, username: String, request: Request[A])
    extends WrappedRequest[A](request)

// Authentication action - checks for valid JWT in Authorization header
class AuthAction @Inject() (
    jwtService: JwtService,
    userRepository: UserRepository,
    override val parser: BodyParsers.Default
)(override implicit val executionContext: ExecutionContext)
    extends ActionBuilder[AuthRequest, AnyContent] {

  // Check and validate JWT token
  override def invokeBlock[A](
      request: Request[A],
      block: AuthRequest[A] => Future[Result]
  ): Future[Result] = {
    // Extract the token from the Authorization header
    // Format: Authorization: Bearer [token]
    val authHeader = request.headers.get("Authorization")

    authHeader match {
      case Some(header) if header.startsWith("Bearer ") =>
        val token = header.substring(7) // Remove "Bearer " prefix

        jwtService.verifyToken(token).flatMap {
          case Some(claim) =>
            Try(claim.subject.toLong) match {
              case Success(userId) =>
                // Execute the block with authenticated request
                block(AuthRequest(userId, claim.username, request))
                  .recover { case e: Exception =>
                    Results.InternalServerError(
                      s"Error during authenticated request: ${e.getMessage}"
                    )
                  }

              case Failure(_) =>
                Future.successful(
                  Results.Unauthorized("Invalid user ID in token")
                )
            }

          case None =>
            Future.successful(Results.Unauthorized("Invalid or expired token"))
        }

      case _ =>
        Future.successful(
          Results.Unauthorized("Missing or invalid Authorization header")
        )
    }
  }
}
