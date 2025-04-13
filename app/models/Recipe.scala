package models

import java.time.ZonedDateTime

// DB entity for Recipe without flavors (used in table mapping)
case class RecipeEntity(
  id: Option[Long] = None,
  name: String,
  description: Option[String] = None,
  userId: Option[Long] = None,
  createdAt: ZonedDateTime = ZonedDateTime.now(),
  updatedAt: ZonedDateTime = ZonedDateTime.now()
)

// Full Recipe with flavors (used for API responses)
case class Recipe(
  id: Option[Long] = None,
  name: String,
  description: Option[String] = None,
  userId: Option[Long] = None,
  flavors: Seq[Flavor] = Seq.empty,
  createdAt: ZonedDateTime = ZonedDateTime.now(),
  updatedAt: ZonedDateTime = ZonedDateTime.now()
)

// Converting between RecipeEntity and Recipe
object Recipe {
  def fromEntity(entity: RecipeEntity, flavors: Seq[Flavor] = Seq.empty): Recipe = 
    Recipe(
      id = entity.id,
      name = entity.name,
      description = entity.description,
      userId = entity.userId,
      flavors = flavors,
      createdAt = entity.createdAt,
      updatedAt = entity.updatedAt
    )
}

case class Flavor(
  id: Option[Long] = None,
  name: String,
  recipeId: Long,
  quantity: Long,
)