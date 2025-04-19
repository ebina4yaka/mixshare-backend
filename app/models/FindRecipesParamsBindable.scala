package models

import play.api.mvc.QueryStringBindable

trait FindRecipesParamsBindable {
  implicit def findRecipesParamsBindable(implicit
      stringBinder: QueryStringBindable[String],
      longBinder: QueryStringBindable[Long],
      intBinder: QueryStringBindable[Int]
  ): QueryStringBindable[FindRecipesParams] =
    FindRecipesParams.queryStringBindable
}