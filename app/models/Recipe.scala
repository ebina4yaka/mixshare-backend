package models

import java.time.ZonedDateTime

import play.api.mvc.QueryStringBindable

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

case class FindRecipesParams(
    keyword: Option[String] = None,
    userId: Option[Long] = None,
    pageSize: Int = 10,
    lastSeen: Option[Long] = None
)

object FindRecipesParams {
  implicit def queryStringBindable(implicit
      longBinder: QueryStringBindable[Long],
      intBinder: QueryStringBindable[Int],
      stringBinder: QueryStringBindable[String]
  ): QueryStringBindable[FindRecipesParams] =
    new QueryStringBindable[FindRecipesParams] {
      override def bind(
          key: String,
          params: Map[String, Seq[String]]
      ): Option[Either[String, FindRecipesParams]] = {
        val keyword = stringBinder.bind("keyword", params)
        val userId = longBinder.bind("userId", params)
        val lastSeen = longBinder.bind("lastSeen", params)
        val pageSize = intBinder.bind("pageSize", params)

        val result = (keyword, userId, lastSeen, pageSize) match {
          case (
                Some(Right(keyword)),
                Some(Right(userId)),
                Some(Right(lastSeen)),
                Some(Right(pageSize))
              ) =>
            Right(
              FindRecipesParams(
                Some(keyword),
                Some(userId),
                pageSize,
                Some(lastSeen)
              )
            )
          case (
                Some(Right(keyword)),
                Some(Right(userId)),
                None,
                Some(Right(pageSize))
              ) =>
            Right(
              FindRecipesParams(Some(keyword), Some(userId), pageSize, None)
            )
          case (Some(Right(keyword)), None, Some(Right(lastSeen)), _) =>
            Right(FindRecipesParams(Some(keyword), None, 10, Some(lastSeen)))
          case (Some(Right(keyword)), None, None, Some(Right(pageSize))) =>
            Right(FindRecipesParams(Some(keyword), None, pageSize, None))
          case (Some(Right(keyword)), _, _, _) =>
            Right(FindRecipesParams(Some(keyword), None))
          case (_, Some(Right(userId)), Some(Right(lastSeen)), _) =>
            Right(FindRecipesParams(None, Some(userId), 10, Some(lastSeen)))
          case (_, Some(Right(userId)), _, Some(Right(pageSize))) =>
            Right(FindRecipesParams(None, Some(userId), pageSize, None))
          case (_, _, Some(Right(lastSeen)), Some(Right(pageSize))) =>
            Right(FindRecipesParams(None, None, pageSize, Some(lastSeen)))
          case (_, _, _, Some(Right(pageSize))) =>
            Right(FindRecipesParams(None, None, pageSize, None))
          case (_, _, _, _) =>
            Right(FindRecipesParams(None, None))
        }

        Option(result)
      }

      override def unbind(key: String, params: FindRecipesParams): String = {
        val base =
          stringBinder.unbind("keyword", params.keyword.getOrElse("")) +
            "&" + longBinder.unbind("userId", params.userId.getOrElse(0L)) +
            "&" + intBinder.unbind("pageSize", params.pageSize)

        params.lastSeen match {
          case Some(id) => base + "&" + longBinder.unbind("lastSeen", id)
          case None     => base
        }
      }
    }
}

// Converting between RecipeEntity and Recipe
object Recipe {
  def fromEntity(
      entity: RecipeEntity,
      flavors: Seq[Flavor] = Seq.empty
  ): Recipe =
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
    quantity: Long
)
