package controllers

import javax.inject.{Inject, Singleton}

import scala.concurrent.ExecutionContext

import org.apache.pekko.stream.scaladsl.StreamConverters
import play.api.mvc.{
  AbstractController,
  Action,
  AnyContent,
  ControllerComponents
}

/** Controller for serving Swagger UI
  */
@Singleton
class SwaggerController @Inject() (cc: ControllerComponents)(implicit
    ec: ExecutionContext
) extends AbstractController(cc) {

  /** Serves the Swagger UI index page
    *
    * @return
    *   The Swagger UI HTML
    */
  def index: Action[AnyContent] = Action {
    Ok(views.html.swagger())
  }

  /** Serves the Swagger JSON spec
    *
    * @return
    *   The Swagger spec as JSON
    */
  def spec: Action[AnyContent] = Action {
    val swaggerFile = new java.io.File("public/swagger.json")
    if (swaggerFile.exists()) {
      Ok.sendFile(swaggerFile).as("application/json")
    } else {
      NotFound("Swagger specification not found")
    }
  }

  def swaggerResources(file: String): Action[AnyContent] = Action {
    val path = s"META-INF/resources/webjars/swagger-ui/5.21.0/$file"
    val resource = getClass.getClassLoader.getResource(path)
    if (resource != null) {
      val contentType = file match {
        case f if f.endsWith(".css") => "text/css"
        case f if f.endsWith(".js")  => "application/javascript"
        case f if f.endsWith(".png") => "image/png"
        case _                       => "text/plain"
      }

      // Convert InputStream to Source for Play 3.x compatibility
      val inputStream = resource.openStream()
      val source =
        org.apache.pekko.stream.scaladsl.StreamConverters.fromInputStream(() =>
          inputStream
        )

      Ok.sendEntity(
        play.api.http.HttpEntity.Streamed(source, None, Some(contentType))
      )
    } else {
      NotFound(s"WebJar resource not found: $path")
    }
  }
}
