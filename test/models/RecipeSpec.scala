package models

import java.time.ZonedDateTime

import helpers.TestHelpers
import org.scalatestplus.play.PlaySpec

class RecipeSpec extends PlaySpec with TestHelpers {

  val now = ZonedDateTime.now()
  
  "Recipe model" should {
    "create a valid Recipe instance" in {
      val recipe = Recipe(
        id = Some(1L),
        name = "Test Recipe",
        description = Some("A test recipe"),
        userId = Some(2L),
        flavors = Seq.empty,
        createdAt = now,
        updatedAt = now
      )
      
      recipe.id mustBe Some(1L)
      recipe.name mustBe "Test Recipe"
      recipe.description mustBe Some("A test recipe")
      recipe.userId mustBe Some(2L)
      recipe.flavors mustBe empty
    }
    
    "create a Recipe with default values" in {
      val recipe = Recipe(
        name = "Minimal Recipe"
      )
      
      recipe.id mustBe None
      recipe.name mustBe "Minimal Recipe"
      recipe.description mustBe None
      recipe.userId mustBe None
      recipe.flavors mustBe empty
      recipe.createdAt must not be null
      recipe.updatedAt must not be null
    }
    
    "include flavors in Recipe" in {
      val flavors = Seq(
        Flavor(Some(1L), "Vanilla", 1L, 10L),
        Flavor(Some(2L), "Chocolate", 1L, 5L)
      )
      
      val recipe = Recipe(
        id = Some(1L),
        name = "Recipe with Flavors",
        flavors = flavors
      )
      
      recipe.flavors must have length 2
      recipe.flavors.head.name mustBe "Vanilla"
      recipe.flavors(1).name mustBe "Chocolate"
    }
  }
  
  "RecipeEntity model" should {
    "create a valid RecipeEntity instance" in {
      val entity = RecipeEntity(
        id = Some(1L),
        name = "Test Entity",
        description = Some("A test entity"),
        userId = Some(2L),
        createdAt = now,
        updatedAt = now
      )
      
      entity.id mustBe Some(1L)
      entity.name mustBe "Test Entity"
      entity.description mustBe Some("A test entity")
      entity.userId mustBe Some(2L)
    }
    
    "create a RecipeEntity with default values" in {
      val entity = RecipeEntity(
        name = "Minimal Entity"
      )
      
      entity.id mustBe None
      entity.name mustBe "Minimal Entity"
      entity.description mustBe None
      entity.userId mustBe None
      entity.createdAt must not be null
      entity.updatedAt must not be null
    }
  }
  
  "Flavor model" should {
    "create a valid Flavor instance" in {
      val flavor = Flavor(
        id = Some(1L),
        name = "Vanilla",
        recipeId = 10L,
        quantity = 5L
      )
      
      flavor.id mustBe Some(1L)
      flavor.name mustBe "Vanilla"
      flavor.recipeId mustBe 10L
      flavor.quantity mustBe 5L
    }
    
    "create a Flavor with minimal values" in {
      val flavor = Flavor(
        name = "Minimal Flavor",
        recipeId = 1L,
        quantity = 1L
      )
      
      flavor.id mustBe None
      flavor.name mustBe "Minimal Flavor"
      flavor.recipeId mustBe 1L
      flavor.quantity mustBe 1L
    }
  }
  
  "Recipe.fromEntity" should {
    "convert RecipeEntity to Recipe with empty flavors" in {
      val entity = RecipeEntity(
        id = Some(1L),
        name = "Test Recipe",
        description = Some("Test description"),
        userId = Some(2L),
        createdAt = now,
        updatedAt = now
      )
      
      val recipe = Recipe.fromEntity(entity)
      
      recipe.id mustBe Some(1L)
      recipe.name mustBe "Test Recipe"
      recipe.description mustBe Some("Test description")
      recipe.userId mustBe Some(2L)
      recipe.flavors mustBe empty
      recipe.createdAt mustBe now
      recipe.updatedAt mustBe now
    }
    
    "convert RecipeEntity to Recipe with provided flavors" in {
      val entity = RecipeEntity(
        id = Some(1L),
        name = "Test Recipe",
        description = Some("Test description"),
        userId = Some(2L),
        createdAt = now,
        updatedAt = now
      )
      
      val flavors = Seq(
        Flavor(Some(1L), "Vanilla", 1L, 10L),
        Flavor(Some(2L), "Chocolate", 1L, 5L)
      )
      
      val recipe = Recipe.fromEntity(entity, flavors)
      
      recipe.id mustBe Some(1L)
      recipe.name mustBe "Test Recipe"
      recipe.flavors must have length 2
      recipe.flavors.head.name mustBe "Vanilla"
      recipe.flavors(1).name mustBe "Chocolate"
    }
  }
  
  "FindRecipesParams" should {
    "create with default values" in {
      val params = FindRecipesParams()
      
      params.keyword mustBe None
      params.userId mustBe None
      params.pageSize mustBe 10
      params.lastSeen mustBe None
    }
    
    "create with custom values" in {
      val params = FindRecipesParams(
        keyword = Some("test"),
        userId = Some(2L),
        pageSize = 20,
        lastSeen = Some(5L)
      )
      
      params.keyword mustBe Some("test")
      params.userId mustBe Some(2L)
      params.pageSize mustBe 20
      params.lastSeen mustBe Some(5L)
    }
  }
}