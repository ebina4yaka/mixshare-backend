package controllers

import java.time.ZonedDateTime
import javax.inject._

import scala.concurrent.ExecutionContext

import models._
import play.api.libs.json._
import play.api.mvc._
import service.RecipeService

@Singleton
class RecipeController @Inject() (
    recipeRepository: RecipeRepository,
    recipeService: RecipeService,
    val controllerComponents: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BaseController {

  // JSON format for Flavor
  implicit val flavorFormat: OFormat[Flavor] = Json.format[Flavor]

  // JSON format for Recipe
  implicit val recipeFormat: OFormat[Recipe] = Json.format[Recipe]

  // JSON format for FindRecipesParams
  implicit val findRecipesParamsFormat: OFormat[FindRecipesParams] =
    Json.format[FindRecipesParams]

  // GET endpoint to retrieve a recipe by ID with its flavors
  def getRecipe(id: Long): Action[AnyContent] = Action.async {
    implicit request =>
      recipeService.getRecipeById(id).map {
        case Some(recipe) => Ok(Json.toJson(recipe))
        case None =>
          NotFound(Json.obj("message" -> s"Recipe with id $id not found"))
      }
  }

  def findRecipes(params: FindRecipesParams): Action[AnyContent] =
    Action.async { implicit request =>
      recipeService.findRecipes(params).map { recipes =>
        Ok(Json.toJson(recipes))
      }
    }
}
