package models

import java.time.ZonedDateTime
import javax.inject.{Inject, Singleton}

import scala.concurrent.{ExecutionContext, Future}

import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import slick.jdbc.PostgresProfile.api.*

@Singleton
class RecipeRepository @Inject() (
    protected val dbConfigProvider: DatabaseConfigProvider
)(implicit ec: ExecutionContext)
    extends HasDatabaseConfigProvider[JdbcProfile] {

  import ZonedDateTimeUtils.* // Import the ZonedDateTime column mapper

  // Recipe table definition
  class RecipeTable(tag: Tag) extends Table[RecipeEntity](tag, "recipe") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")
    def description = column[Option[String]]("description")
    def userId = column[Option[Long]]("user_id")
    def createdAt = column[ZonedDateTime]("created_at")
    def updatedAt = column[ZonedDateTime]("updated_at")

    def * = (id.?, name, description, userId, createdAt, updatedAt)
      .mapTo[RecipeEntity]
  }

  // Flavor table definition
  class FlavorTable(tag: Tag) extends Table[Flavor](tag, "flavor") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")
    def recipeId = column[Long]("recipe_id")
    def quantity = column[Long]("quantity")

    def * = (id.?, name, recipeId, quantity).mapTo[Flavor]

    def recipe = foreignKey("fk_recipe", recipeId, recipes)(
      _.id,
      onUpdate = ForeignKeyAction.Restrict,
      onDelete = ForeignKeyAction.Cascade
    )
  }

  val recipes = TableQuery[RecipeTable]
  val flavors = TableQuery[FlavorTable]

  // Get recipe by ID with its flavors
  def getById(id: Long): Future[Option[Recipe]] = {
    val recipeQuery = recipes.filter(_.id === id)
    val flavorQuery = flavors.filter(_.recipeId === id)

    for {
      recipeOpt <- db.run(recipeQuery.result.headOption)
      flavorsForRecipe <- db.run(flavorQuery.result)
    } yield {
      recipeOpt.map(recipeEntity =>
        Recipe.fromEntity(recipeEntity, flavorsForRecipe)
      )
    }
  }

  def getByIds(ids: Seq[Long]): Future[Seq[Recipe]] = {
    val recipeQuery = recipes.filter(_.id inSet ids)

    for {
      recipeEntities <- db.run(recipeQuery.result)
      recipeIds = recipeEntities.flatMap(_.id)
      allFlavors <- db.run(flavors.filter(_.recipeId inSet recipeIds).result)
    } yield {
      recipeEntities.map { entity =>
        val recipeFlavors =
          allFlavors.filter(_.recipeId == entity.id.getOrElse(0L))
        Recipe.fromEntity(entity, recipeFlavors)
      }
    }
  }

  // Search recipes by keyword and optional filters with seek pagination
  def findRecipeIds(params: FindRecipesParams): Future[Seq[Long]] = {
    var query = recipes.filterOpt(params.keyword) { case (r, keyword) =>
      r.name like s"%$keyword%"
    }

    query = query.filterOpt(params.userId) { case (r, userId) =>
      r.userId === userId
    }

    // Apply seek method pagination if lastSeen ID is provided
    query = params.lastSeen match {
      case Some(lastId) =>
        // Filter for IDs greater than the last seen ID (assuming ID is monotonically increasing)
        query.filter(_.id > lastId).sortBy(_.id.asc)
      case None =>
        // First page - just sort by ID
        query.sortBy(_.id.asc)
    }

    // Limit to pageSize
    query = query.take(params.pageSize)

    db.run(query.map(_.id).result).map(_.collect { case id if id > 0 => id })
  }

  // Create a new recipe with flavors
  def create(recipe: Recipe): Future[Recipe] = {
    val now = ZonedDateTime.now()
    val recipeEntity = RecipeEntity(
      id = recipe.id,
      name = recipe.name,
      description = recipe.description,
      userId = recipe.userId,
      createdAt = now,
      updatedAt = now
    )

    val insertRecipeAction =
      (recipes returning recipes.map(_.id)) += recipeEntity

    db.run(insertRecipeAction.transactionally).flatMap { recipeId =>
      val flavorsWithRecipeId =
        recipe.flavors.map(flavor => flavor.copy(recipeId = recipeId))

      if (flavorsWithRecipeId.nonEmpty) {
        val insertFlavorsAction = flavors ++= flavorsWithRecipeId
        db.run(insertFlavorsAction)
          .map(_ =>
            Recipe.fromEntity(
              recipeEntity.copy(id = Some(recipeId)),
              flavorsWithRecipeId
            )
          )
      } else {
        Future.successful(
          Recipe.fromEntity(recipeEntity.copy(id = Some(recipeId)), Seq.empty)
        )
      }
    }
  }

  // Update an existing recipe
  def update(recipe: Recipe): Future[Option[Recipe]] = {
    recipe.id match {
      case Some(id) =>
        val now = ZonedDateTime.now()
        val recipeEntity = RecipeEntity(
          id = recipe.id,
          name = recipe.name,
          description = recipe.description,
          userId = recipe.userId,
          createdAt = recipe.createdAt,
          updatedAt = now
        )

        val updateRecipeAction =
          recipes.filter(_.id === id).update(recipeEntity)
        val deleteFlavorsAction = flavors.filter(_.recipeId === id).delete
        val insertFlavorsAction =
          flavors ++= recipe.flavors.map(_.copy(recipeId = id))

        val combinedAction = for {
          updateResult <- updateRecipeAction
          _ <- deleteFlavorsAction
          _ <-
            if (recipe.flavors.nonEmpty) insertFlavorsAction
            else DBIO.successful(Seq.empty)
        } yield updateResult

        db.run(combinedAction.transactionally).flatMap { updateCount =>
          if (updateCount > 0) getById(id)
          else Future.successful(None)
        }

      case None => Future.successful(None)
    }
  }

  // Delete a recipe and its flavors
  def delete(id: Long): Future[Boolean] = {
    val deleteAction = recipes.filter(_.id === id).delete

    db.run(deleteAction).map { affectedRows =>
      affectedRows > 0
    }
  }
}
