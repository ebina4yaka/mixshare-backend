package controllers

import javax.inject.*

import play.api.mvc.*

@Singleton
class HomeController @Inject() (val controllerComponents: ControllerComponents)
    extends BaseController {

  def hello() = Action { implicit request: Request[AnyContent] =>
    Ok("hello!")
  }
}
