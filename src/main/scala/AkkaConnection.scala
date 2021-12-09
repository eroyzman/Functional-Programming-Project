import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.scaladsl.Source
import play.api.libs.json._

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration.{DurationInt, FiniteDuration}

object AkkaConnection extends App {

  implicit val system: ActorSystem[Nothing] = {
    ActorSystem(Behaviors.empty, "SingleRequest")
  }
  // needed for the future flatMap/onComplete in the end
  implicit val executionContext: ExecutionContextExecutor = {
    system.executionContext
  }

  Source.tick(0.second, 5.second, 1).mapAsync(1)(_ => callHttp().map(value => (value \ "items").validate[Seq[JsValue]] match {
    case JsSuccess(items, _) =>
      items foreach {
        items =>
          println((items \ "name").as[String])
          println((items \ "path").as[String])
          println((items \ "sha").as[String])
          println((items \ "url").as[String])
          println((items \ "git_url").as[String])
          println((items \ "html_url").as[String])
      }
    case error: JsError =>
      println(error)
  }
  )).run()

  val authorization = headers.Authorization(BasicHttpCredentials("token", "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"))

  def callHttp() = {
    Http().singleRequest(HttpRequest(
      method = HttpMethods.GET,
      uri = Uri("https://api.github.com/search/code") withQuery ("q", "addClass") +: ("in", "file") +: Query.Empty,
      headers = List(authorization)
    )
    ).flatMap { res =>
      Unmarshal(res).to[String].map { data =>
        Json.parse(data)
      }
    }
  }


  case class passApiResult(name: String, path: String, sha: String, url: String, git_url: String, html_url: String)
}
