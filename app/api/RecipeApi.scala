package api

import api.ApiTypes._
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.json.play._

object RecipeApi {

  // Base API endpoint
  private val baseEndpoint = endpoint
    .in("api" / "recipes")
    .errorOut(statusCode and plainBody[String])

  // Get recipe by ID endpoint
  val getRecipeEndpoint = baseEndpoint.get
    .in(path[Long]("id"))
    .out(statusCode(StatusCode.Ok) and jsonBody[Recipe])
    .errorOut(statusCode(StatusCode.NotFound) and jsonBody[NotFoundResponse])
    .name("getRecipe")
    .description("Get recipe by ID with its flavors")
    .tag("Recipes")

  // Find recipes endpoint with query parameters
  val findRecipesEndpoint = baseEndpoint.get
    .in(query[Option[String]]("name").description("Filter by recipe name"))
    .in(
      query[Option[String]]("flavorName").description("Filter by flavor name")
    )
    .in(
      query[Option[Int]]("maxCookingTime").description(
        "Maximum cooking time in minutes"
      )
    )
    .in(
      query[Option[Int]]("minServings").description(
        "Minimum number of servings"
      )
    )
    .out(statusCode(StatusCode.Ok) and jsonBody[Seq[Recipe]])
    .name("findRecipes")
    .description("Find recipes with optional filters")
    .tag("Recipes")

  // All recipe endpoints
  val endpoints = List(
    getRecipeEndpoint,
    findRecipesEndpoint
  )
}
