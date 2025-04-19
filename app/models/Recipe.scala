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
    page: Int = 1,
    pageSize: Int = 10
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
        val page = intBinder.bind("page", params)
        val pageSize = intBinder.bind("pageSize", params)

        val result = (keyword, userId, page, pageSize) match {
          case (
                Some(Right(keyword)),
                Some(Right(userId)),
                Some(Right(page)),
                Some(Right(pageSize))
              ) =>
            Right(
              FindRecipesParams(Some(keyword), Some(userId), page, pageSize)
            )
          case (Some(Right(keyword)), _, _, _) =>
            Right(FindRecipesParams(Some(keyword), None))
          case (_, Some(Right(userId)), _, _) =>
            Right(FindRecipesParams(None, Some(userId)))
          case (_, _, Some(Right(page)), Some(Right(pageSize))) =>
            Right(FindRecipesParams(None, None, page, pageSize))
          case (_, _, _, _) =>
            Right(FindRecipesParams(None, None))
        }

        Option(result)
      }

      override def unbind(key: String, params: FindRecipesParams): String = {
        stringBinder.unbind("keyword", params.keyword.getOrElse("")) +
          "&" + longBinder.unbind("userId", params.userId.getOrElse(0L)) +
          "&" + intBinder.unbind("page", params.page) +
          "&" + intBinder.unbind("pageSize", params.pageSize)
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
