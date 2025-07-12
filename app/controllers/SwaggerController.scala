package controllers

import javax.inject._

import scala.concurrent.Future

import api.{AuthApi, RecipeApi, UserApi}
import play.api.mvc._
import sttp.apispec.openapi.circe.yaml._
import sttp.tapir.docs.openapi.OpenAPIDocsInterpreter
import sttp.tapir.swagger.bundle.SwaggerInterpreter

@Singleton
class SwaggerController @Inject() (
    val controllerComponents: ControllerComponents
) extends BaseController {

  // すべてのエンドポイントを収集
  private val allEndpoints =
    AuthApi.endpoints ++
      UserApi.endpoints ++
      RecipeApi.endpoints

  // OpenAPI仕様を生成
  private val openApiDocs = OpenAPIDocsInterpreter()
    .toOpenAPI(allEndpoints, "SS API", "1.0.0")

  // OpenAPI YAML形式で仕様を返すエンドポイント
  def openApiYaml: Action[AnyContent] = Action { implicit request =>
    Ok(openApiDocs.toYaml: String).as("application/yaml")
  }

  // OpenAPI JSON形式で仕様を返すエンドポイント
  def openApiJson: Action[AnyContent] = Action { implicit request =>
    import io.circe.syntax._
    import sttp.apispec.openapi.circe._
    Ok(openApiDocs.asJson.spaces2: String).as("application/json")
  }

  // Swagger UIを提供するエンドポイント
  def swaggerUI: Action[AnyContent] = Action { implicit request =>
    val swaggerUIEndpoints = SwaggerInterpreter()
      .fromEndpoints[Future](allEndpoints, "SS API", "1.0.0")

    // Swagger UIのHTMLを生成
    val swaggerHTML = swaggerUIEndpoints.headOption match {
      case Some(endpoint) =>
        """<!DOCTYPE html>
           |<html>
           |<head>
           |  <title>SS API Documentation</title>
           |  <link rel="stylesheet" type="text/css" href="https://unpkg.com/swagger-ui-dist@5.21.0/swagger-ui.css" />
           |  <style>
           |    html { box-sizing: border-box; overflow: -moz-scrollbars-vertical; overflow-y: scroll; }
           |    *, *:before, *:after { box-sizing: inherit; }
           |    body { margin:0; background: #fafafa; }
           |  </style>
           |</head>
           |<body>
           |  <div id="swagger-ui"></div>
           |  <script src="https://unpkg.com/swagger-ui-dist@5.21.0/swagger-ui-bundle.js"></script>
           |  <script src="https://unpkg.com/swagger-ui-dist@5.21.0/swagger-ui-standalone-preset.js"></script>
           |  <script>
           |    window.onload = function() {
           |      const ui = SwaggerUIBundle({
           |        url: "/api/docs/openapi.yaml",
           |        dom_id: '#swagger-ui',
           |        deepLinking: true,
           |        presets: [
           |          SwaggerUIBundle.presets.apis,
           |          SwaggerUIStandalonePreset
           |        ],
           |        plugins: [
           |          SwaggerUIBundle.plugins.DownloadUrl
           |        ],
           |        layout: "StandaloneLayout"
           |      });
           |    };
           |  </script>
           |</body>
           |</html>""".stripMargin
      case None =>
        "<html><body><h1>No API endpoints found</h1></body></html>"
    }

    Ok(swaggerHTML).as("text/html")
  }
}
