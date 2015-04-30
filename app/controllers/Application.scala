package controllers

import play.api._
import play.api.Play.current
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import play.api.libs.ws.WS
import play.api.mvc._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.mutable
import scala.concurrent.Future

object Application extends Controller {

  case class Feed(id: String, name: String, contribs: Seq[Contrib])
  implicit val feedReads: Reads[Future[Feed]] = (
      (__ \ "_id").read[String] ~
      (__ \ "name").read[String]
    ).apply((id, name) => {
    Branchr.getContribs(id).map(contribs => Feed(id, name, contribs))
  })

  case class Contrib(id: String, params: JsObject, engine: Engine)
  implicit val contribReads: Reads[Future[Contrib]] = (
      (__ \ "_id").read[String] ~
      (__ \ "params").read[JsObject] ~
      (__ \ "engineId").read[String]
    ).apply((id, params, engineId) => {
    Branchr.getEngine(engineId).map(engine => Contrib(id, params, engine))
  })

  case class Engine(id: String, js: String, html: String, css: String)
  implicit val engineReads: Reads[Engine] = (
      (__ \ "_id").read[String] ~
      (__ \ "js").read[String] ~
      (__ \ "html").read[String] ~
      (__ \ "css").read[String]
    ).apply(Engine)

  object Branchr {
    val engineCache: mutable.Map[String, Engine] = mutable.Map()
    val base = "https://branchr.herokuapp.com"
    def getFeeds(): Future[Seq[Feed]] =
      WS.url(s"$base/feed/").get().flatMap(resp => {
        Future.sequence(resp.json.as[Seq[Future[Feed]]])
      })
    def getFeed(feedId: String): Future[Feed] =
      WS.url(s"$base/feed/$feedId").get().flatMap(resp => {
        resp.json.as[Future[Feed]]
      })
    def getContribs(feedId: String): Future[Seq[Contrib]] =
      WS.url(s"$base/contrib?feedId=$feedId").get().flatMap(resp => {
        Future.sequence(resp.json.as[Seq[Future[Contrib]]])
      })
    def getEngine(engineId: String): Future[Engine] =
      engineCache.get(engineId) match {
        case Some(engine) => Future(engine)
        case None =>
          WS.url(s"$base/engine/$engineId").get().map(resp => {
            val e = resp.json.as[Engine]
            engineCache += (e.id -> e)
            e
          })
    }
  }

  def feeds = Action.async {
    for {
      feeds <- Branchr.getFeeds()
    } yield {
      Ok(views.html.feeds(feeds))
    }
  }

  def feed(id: String) = Action.async {
    for {
      feed <- Branchr.getFeed(id)
      contribs <- Branchr.getContribs(id)
    } yield {
      Ok(views.html.feed(feed, contribs))
    }
  }

}