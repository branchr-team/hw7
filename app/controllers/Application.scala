package controllers

import play.api._
import play.api.Play.current
import play.api.libs.ws.WS
import play.api.mvc._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object Application extends Controller {

  object Branchr {
    val base = "https://branchr.herokuapp.com"
    def getFeeds() = WS.url(s"$base/feed/").get()
    def getEngine(engineId: String) = WS.url(s"$base/engine/$engineId")
  }

  def foo = Action.async {
      Branchr.getFeeds().flatMap(resp => {
        val contrib = resp.json(0)
        val engineId = (contrib \ "engineId").as[String]
        Branchr.getEngine(engineId).get().map(resp2 => {
          Ok(views.html.foo(resp2.body))
        })
      })
  }

}