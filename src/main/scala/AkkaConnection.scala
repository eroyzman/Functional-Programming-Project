import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.scaladsl.Source
import play.api.libs.json._
import scala.util.{Failure, Success}

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

  Source.tick(0.second, 15.seconds, 1).mapAsync(1)(_ => callHttp().map(value => (value \ "items").validate[Seq[JsValue]] match {
    case JsSuccess(items, _) =>
      items foreach {
        items =>
          //First, get repo of searched item
          Http().singleRequest(HttpRequest(
            method = HttpMethods.GET,
            uri = Uri(s"https://api.github.com/repos/${(items \ "repository" \ "owner" \ "login").as[String]}/${(items \ "repository" \ "name").as[String]}/contents/${(items \ "path").as[String]}".replace(" ", "%20")),
            headers = List(authorization)))
            .flatMap { response =>
              Unmarshal(response).to[String].map { data =>
                Json.parse(data)
              }
            }
            .onComplete {
              case Success(res) =>
                //After get raw file
                Http().singleRequest(HttpRequest(
                  method = HttpMethods.GET,
                  uri = Uri(s"${(res \ "download_url").as[String]}"),
                  headers = List(authorization))).flatMap {
                  res =>
                    Unmarshal(res).to[String].map { data =>
                      //parse our search value
                      println(data.slice(data.indexOfSlice("AWSAccessKeyId"), data.indexOfSlice("AWSAccessKeyId") + 100)) //test ranges
                    }
                }
              case Failure(e) =>
                println(s"Failure: $e")
            }
      }
    case error: JsError =>
      println(error)
  }
  )).run()

  val authorization = headers.Authorization(BasicHttpCredentials("token", "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"))

  def callHttp() = {
    Http().singleRequest(HttpRequest(
      method = HttpMethods.GET,
      uri = Uri("https://api.github.com/search/code") withQuery ("q", "AWSAccessKeyId") +: ("in", "file") +: Query.Empty,
      headers = List(authorization)
    )
    ).flatMap { res =>
      Unmarshal(res).to[String].map { data =>
        Json.parse(data)
      }
    }
  }


//  case class passApiResult(name: String, path: String, sha: String, url: String, git_url: String, html_url: String)
}
