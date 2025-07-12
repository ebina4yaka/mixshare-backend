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
    .in(
      query[Option[String]]("keyword").description("Filter by search keyword")
    )
    .in(
      query[Option[Long]]("userId").description("Filter by user id")
    )
    .in(
      query[Option[Int]]("pageSize").description("page size")
    )
    .in(
      query[Option[Long]]("lastSeen").description("last seen")
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
