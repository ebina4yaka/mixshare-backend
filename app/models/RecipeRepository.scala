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
}
