package controllers

import java.time.ZonedDateTime

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import helpers.{TestHelpers, TestWithDBCleaner}
import models.{FindRecipesParams, Recipe, RecipeRepository}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsArray, Json}
import play.api.test.Helpers._
import play.api.test._
import service.RecipeService

class RecipeControllerSpec
    extends TestWithDBCleaner
    with GuiceOneAppPerSuite
    with MockitoSugar
    with BeforeAndAfterEach {

  val mockRecipeRepository: RecipeRepository = mock[RecipeRepository]
  val mockRecipeService: RecipeService = mock[RecipeService]

  override def fakeApplication(): Application = {
    GuiceApplicationBuilder()
      .overrides(
        bind[RecipeRepository].toInstance(mockRecipeRepository),
        bind[RecipeService].toInstance(mockRecipeService),
        bind[services.MockModule].toInstance(new services.MockModule)
      )
      .build()
  }

  override def beforeEach(): Unit = {
    reset(mockRecipeRepository, mockRecipeService)
  }

  "RecipeController getRecipe" should {
    "return 200 with recipe data when recipe exists" in {
      val now = ZonedDateTime.now()
      val recipe = Recipe(
        Some(1L),
        "Test Recipe",
        Some("Recipe description"),
        Some(1L),
        Seq.empty,
        now,
        now
      )

      // Use a mock implementation based on actual service methods
      when(mockRecipeService.getRecipeById(any[Long]))
        .thenReturn(Future.successful(Some(recipe)))

      val request = FakeRequest(GET, "/api/recipes/1")
      val result = route(app, request).get

      status(result) mustBe OK
      (contentAsJson(result) \ "name").as[String] mustBe "Test Recipe"
    }

    "return 404 when recipe does not exist" in {
      when(mockRecipeService.getRecipeById(any[Long]))
        .thenReturn(Future.successful(None))

      val request = FakeRequest(GET, "/api/recipes/999")
      val result = route(app, request).get

      status(result) mustBe NOT_FOUND
    }
  }

  "RecipeController findRecipes" should {
    "return 200 with list of recipes" in {
      val now = ZonedDateTime.now()
      val recipes = Seq(
        Recipe(
          Some(1L),
          "Recipe 1",
          Some("Description 1"),
          Some(1L),
          Seq.empty,
          now,
          now
        ),
        Recipe(
          Some(2L),
          "Recipe 2",
          Some("Description 2"),
          Some(1L),
          Seq.empty,
          now,
          now
        )
      )

      when(mockRecipeService.findRecipes(any[FindRecipesParams]))
        .thenReturn(Future.successful(recipes))

      val request = FakeRequest(GET, "/api/recipes?pageSize=10")
      val result = route(app, request).get

      status(result) mustBe OK
      contentAsJson(result).as[JsArray].value.size mustEqual 2
    }

    "apply filters correctly" in {
      val now = ZonedDateTime.now()
      val recipes = Seq(
        Recipe(
          Some(1L),
          "Filtered Recipe",
          Some("Description with keyword"),
          Some(1L),
          Seq.empty,
          now,
          now
        )
      )

      // Use simple stubbing instead of argument capture
      when(mockRecipeService.findRecipes(any[FindRecipesParams]))
        .thenReturn(Future.successful(recipes))

      val request =
        FakeRequest(GET, "/api/recipes?keyword=keyword&userId=1&pageSize=5")
      val result = route(app, request).get

      status(result) mustBe OK
      contentAsJson(result).as[JsArray].value.size mustEqual 1
    }
  }
}
