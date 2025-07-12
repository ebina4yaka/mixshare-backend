package controllers

import java.time.ZonedDateTime
import javax.inject._

import scala.concurrent.ExecutionContext

import api.{ApiTypes, RecipeApi}
import models.{
  FindRecipesParams => ModelsFindRecipesParams,
  Recipe => ModelsRecipe,
  _
}
import play.api.libs.json._
import play.api.mvc._
import service.RecipeService
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.json.play._

@Singleton
class RecipeController @Inject() (
    recipeRepository: RecipeRepository,
    recipeService: RecipeService,
    val controllerComponents: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BaseController {

  import ApiTypes._

  // Temporary workaround - convert models.Recipe to ApiTypes.Recipe
  private def toApiRecipe(recipe: ModelsRecipe): ApiTypes.Recipe = {
    ApiTypes.Recipe(
      id = recipe.id,
      name = recipe.name,
      description = recipe.description,
      createdAt = recipe.createdAt,
      updatedAt = recipe.updatedAt,
      flavors = recipe.flavors.map(f =>
        ApiTypes.Flavor(
          id = f.id,
          recipeId = f.recipeId,
          name = f.name,
          quantity = f.quantity
        )
      )
    )
  }

  // GET endpoint to retrieve a recipe by ID with its flavors
  def getRecipe(id: Long): Action[AnyContent] = Action.async {
    implicit request =>
      recipeService.getRecipeById(id).map {
        case Some(recipe) => Ok(Json.toJson(toApiRecipe(recipe)))
        case None =>
          NotFound(
            Json.toJson(NotFoundResponse(s"Recipe with id $id not found"))
          )
      }
  }

  def findRecipes(): Action[AnyContent] = Action.async { implicit request =>
    val keyword = request.getQueryString("keyword")
    val userId = request.getQueryString("userId")
    val pageSize = request.getQueryString("pageSize")
    val lastSeen = request.getQueryString("lastSeen")

    val params = ModelsFindRecipesParams(
      keyword = keyword,
      userId = userId match
        case Some(userId) => Some(userId.toLong),
      pageSize = pageSize match
        case Some(pageSize) => pageSize.toInt,
      lastSeen = lastSeen match
        case Some(lastSeen) => Some(lastSeen.toLong)
    )

    recipeService.findRecipes(params).map { recipes =>
      Ok(Json.toJson(recipes.map(toApiRecipe)))
    }
  }
}
