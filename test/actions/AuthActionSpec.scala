package actions

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import helpers.TestHelpers
import models.UserRepository
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.Materializer
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.mvc._
import play.api.test.Helpers._
import play.api.test._
import service.{JwtClaim, JwtService}

class AuthActionSpec extends PlaySpec
    with GuiceOneAppPerSuite
    with MockitoSugar
    with BeforeAndAfterEach
    with TestHelpers {

  implicit val system: ActorSystem = ActorSystem("test")
  implicit val materializer: Materializer = Materializer(system)
        
  val mockJwtService: JwtService = mock[JwtService]
  val mockUserRepository: UserRepository = mock[UserRepository]
  val bodyParser: BodyParsers.Default = app.injector.instanceOf[BodyParsers.Default]
  
  val authAction = new AuthAction(mockJwtService, mockUserRepository, bodyParser)
  
  override def beforeEach(): Unit = {
    reset(mockJwtService, mockUserRepository)
  }
  
  // Define a simple test controller action that uses AuthAction
  def testController(userId: Long, username: String): Action[AnyContent] = {
    authAction { request =>
      Results.Ok(s"User ID: ${request.userId}, Username: ${request.username}")
    }
  }
  
  "AuthAction" should {
    "allow access with valid token" in {
      // Setup
      val validToken = "valid.test.token"
      val validClaim = JwtClaim(
        subject = "123",
        username = "testuser",
        jwtId = Some("abc123"),
        tokenType = "access"
      )
      
      when(mockJwtService.verifyToken(validToken))
        .thenReturn(Future.successful(Some(validClaim)))
      
      // Create a test request with Authorization header
      val request = FakeRequest(GET, "/api/secure")
        .withHeaders(AUTHORIZATION -> s"Bearer $validToken")
      
      // Execute the action
      val result = call(testController(123L, "testuser"), request)
      
      // Verify the response
      status(result) mustBe OK
      contentAsString(result) mustBe "User ID: 123, Username: testuser"
    }
    
    "reject request with missing Authorization header" in {
      // Create a test request without Authorization header
      val request = FakeRequest(GET, "/api/secure")
      
      // Execute the action
      val result = call(testController(1L, "testuser"), request)
      
      // Verify the response
      status(result) mustBe UNAUTHORIZED
      contentAsString(result) must include("Missing or invalid Authorization header")
    }
    
    "reject request with malformed Authorization header" in {
      // Create a test request with malformed Authorization header
      val request = FakeRequest(GET, "/api/secure")
        .withHeaders(AUTHORIZATION -> "malformed-header")
      
      // Execute the action
      val result = call(testController(1L, "testuser"), request)
      
      // Verify the response
      status(result) mustBe UNAUTHORIZED
      contentAsString(result) must include("Missing or invalid Authorization header")
    }
    
    "reject request with invalid token" in {
      // Setup
      val invalidToken = "invalid.test.token"
      
      when(mockJwtService.verifyToken(invalidToken))
        .thenReturn(Future.successful(None))
      
      // Create a test request with invalid token
      val request = FakeRequest(GET, "/api/secure")
        .withHeaders(AUTHORIZATION -> s"Bearer $invalidToken")
      
      // Execute the action
      val result = call(testController(1L, "testuser"), request)
      
      // Verify the response
      status(result) mustBe UNAUTHORIZED
      contentAsString(result) must include("Invalid or expired token")
    }
    
    "reject request with invalid user ID in token" in {
      // Setup
      val tokenWithInvalidUserId = "token.with.invalid.userid"
      val invalidClaim = JwtClaim(
        subject = "not-a-number", // This will cause toLong to fail
        username = "testuser",
        jwtId = Some("abc123"),
        tokenType = "access"
      )
      
      when(mockJwtService.verifyToken(tokenWithInvalidUserId))
        .thenReturn(Future.successful(Some(invalidClaim)))
      
      // Create a test request with token containing invalid user ID
      val request = FakeRequest(GET, "/api/secure")
        .withHeaders(AUTHORIZATION -> s"Bearer $tokenWithInvalidUserId")
      
      // Execute the action
      val result = call(testController(1L, "testuser"), request)
      
      // Verify the response
      status(result) mustBe UNAUTHORIZED
      contentAsString(result) must include("Invalid user ID in token")
    }
  }
}