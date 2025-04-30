package services

import java.time.ZonedDateTime

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

import helpers.TestHelpers
import models.{
  FindRecipesParams,
  Flavor,
  Recipe,
  RecipeEntity,
  RecipeRepository
}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.test.Injecting
import service.RecipeService

class RecipeServiceSpec
    extends PlaySpec
    with GuiceOneAppPerSuite
    with MockitoSugar
    with BeforeAndAfterEach
    with ScalaFutures
    with Injecting
    with TestHelpers {

  val mockRecipeRepository: RecipeRepository = mock[RecipeRepository]

  // Create the service with mocked dependencies
  val recipeService = new RecipeService(mockRecipeRepository)

  // Test data
  val now = ZonedDateTime.now()
  val testFlavor1 = Flavor(Some(1L), "Strawberry", 1L, 10L)
  val testFlavor2 = Flavor(Some(2L), "Blueberry", 1L, 5L)

  val testRecipe = Recipe(
    id = Some(1L),
    name = "Fruit Mix",
    description = Some("A delicious fruit mix"),
    userId = Some(1L),
    flavors = Seq(testFlavor1, testFlavor2),
    createdAt = now,
    updatedAt = now
  )

  override def beforeEach(): Unit = {
    reset(mockRecipeRepository)
  }

  "RecipeService" should {
    "find recipes with parameters" in {
      // Setup
      val params = FindRecipesParams(
        keyword = Some("fruit"),
        userId = Some(1L),
        pageSize = 10
      )
      val recipeIds = Seq(1L, 2L)
      val recipes = Seq(
        testRecipe,
        testRecipe.copy(id = Some(2L), name = "Another Fruit Mix")
      )

      when(mockRecipeRepository.findRecipeIds(params))
        .thenReturn(Future.successful(recipeIds))
      when(mockRecipeRepository.getByIds(recipeIds))
        .thenReturn(Future.successful(recipes))

      // When
      val resultFuture = recipeService.findRecipes(params)
      val result = Await.result(resultFuture, 5.seconds)

      // Then
      result must have length 2
      result.head.name must include("Fruit")
      result(1).name must include("Fruit")
    }

    "return empty list when no recipe IDs found" in {
      // Setup
      val params = FindRecipesParams(keyword = Some("nonexistent"))

      when(mockRecipeRepository.findRecipeIds(params))
        .thenReturn(Future.successful(Seq.empty))

      // When
      val resultFuture = recipeService.findRecipes(params)
      val result = Await.result(resultFuture, 5.seconds)

      // Then
      result mustBe empty

      // Verify getByIds was not called
      verify(mockRecipeRepository, never()).getByIds(any[Seq[Long]])
    }

    "get recipe by ID" in {
      // Setup
      when(mockRecipeRepository.getById(1L))
        .thenReturn(Future.successful(Some(testRecipe)))

      // When
      val resultFuture = recipeService.getRecipeById(1L)
      val result = Await.result(resultFuture, 5.seconds)

      // Then
      result must not be None
      result.get.id mustBe Some(1L)
      result.get.name mustBe "Fruit Mix"
      result.get.flavors must have length 2
    }

    "return None when recipe not found" in {
      // Setup
      when(mockRecipeRepository.getById(999L))
        .thenReturn(Future.successful(None))

      // When
      val resultFuture = recipeService.getRecipeById(999L)
      val result = Await.result(resultFuture, 5.seconds)

      // Then
      result mustBe None
    }

    "create recipe" in {
      // Setup
      val newRecipe = testRecipe.copy(id = None)
      val createdRecipe = testRecipe

      when(mockRecipeRepository.create(newRecipe))
        .thenReturn(Future.successful(createdRecipe))

      // When
      val resultFuture = recipeService.createRecipe(newRecipe)
      val result = Await.result(resultFuture, 5.seconds)

      // Then
      result.id mustBe Some(1L)
      result.name mustBe "Fruit Mix"
      result.flavors must have length 2
    }

    "update recipe" in {
      // Setup
      val updatedRecipe = testRecipe.copy(
        name = "Updated Fruit Mix",
        description = Some("Updated description")
      )

      when(mockRecipeRepository.update(updatedRecipe))
        .thenReturn(Future.successful(Some(updatedRecipe)))

      // When
      val resultFuture = recipeService.updateRecipe(updatedRecipe)
      val result = Await.result(resultFuture, 5.seconds)

      // Then
      result must not be None
      result.get.name mustBe "Updated Fruit Mix"
      result.get.description mustBe Some("Updated description")
    }

    "return None when updating non-existent recipe" in {
      // Setup
      val nonExistentRecipe = testRecipe.copy(id = Some(999L))

      when(mockRecipeRepository.update(nonExistentRecipe))
        .thenReturn(Future.successful(None))

      // When
      val resultFuture = recipeService.updateRecipe(nonExistentRecipe)
      val result = Await.result(resultFuture, 5.seconds)

      // Then
      result mustBe None
    }

    "delete recipe" in {
      // Setup
      when(mockRecipeRepository.delete(1L))
        .thenReturn(Future.successful(true))

      // When
      val resultFuture = recipeService.deleteRecipe(1L)
      val result = Await.result(resultFuture, 5.seconds)

      // Then
      result mustBe true
    }

    "return false when deleting non-existent recipe" in {
      // Setup
      when(mockRecipeRepository.delete(999L))
        .thenReturn(Future.successful(false))

      // When
      val resultFuture = recipeService.deleteRecipe(999L)
      val result = Await.result(resultFuture, 5.seconds)

      // Then
      result mustBe false
    }
  }
}
