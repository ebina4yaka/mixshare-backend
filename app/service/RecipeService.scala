package service

import javax.inject.{Inject, Singleton}

import scala.concurrent.{ExecutionContext, Future}

import models.{FindRecipesParams, Recipe, RecipeRepository}

@Singleton
class RecipeService @Inject() (
    recipeRepository: RecipeRepository
)(implicit ec: ExecutionContext) {

  def findRecipes(params: FindRecipesParams): Future[Seq[Recipe]] = {
    recipeRepository.findRecipeIds(params).flatMap { ids =>
      if (ids.isEmpty) Future.successful(Seq.empty)
      else recipeRepository.getByIds(ids)
    }
  }

  def getRecipeById(id: Long): Future[Option[Recipe]] = {
    recipeRepository.getById(id)
  }

  def createRecipe(recipe: Recipe): Future[Recipe] = {
    recipeRepository.create(recipe)
  }

  def updateRecipe(recipe: Recipe): Future[Option[Recipe]] = {
    recipeRepository.update(recipe)
  }

  def deleteRecipe(id: Long): Future[Boolean] = {
    recipeRepository.delete(id)
  }
}
