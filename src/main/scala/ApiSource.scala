import Application.system
import Application.system.dispatcher
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Sink, Source}
import play.api.libs.json.{JsSuccess, JsValue, Json}

import scala.util.{Failure, Success}
//import scala.concurrent.duration.{DurationInt, FiniteDuration}
import akka.actor.{ActorSystem, Cancellable}

import scala.concurrent.duration._

object ApiSource {
  val authorization = headers.Authorization(BasicHttpCredentials("token", "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"))
  val SEARCH: Uri = Uri("https://api.github.com/search/code") withQuery ("q", "AWSAccessKeyId") +: ("in", "file") +: Query.Empty
  val delayBased: Source[String, Cancellable] = Source.tick(initialDelay = 1.second,
    interval = 15.seconds,
    tick = "I'll be sent after the delay").mapAsync(1)(_ =>
    Http().singleRequest(HttpRequest(
      method = HttpMethods.GET,
      uri = Uri("https://api.github.com/search/code"))).flatMap {
      response =>
        println(response)
        Unmarshal(response).to[String]
    }
  )

  val gitHubApiSource: Source[String, Cancellable] = Source.tick(1.second, 15.seconds, 1)
    .wireTap(data => println(s" Before map Async${data}"))
    .mapAsync(1)(_ =>
      getHttpRequest(SEARCH)
        .map { data => Json.parse(data) }
        .map { value =>
          (value \ "items")
            .validate[Seq[JsValue]] match {
            case JsSuccess(items, _) =>
              //              println(items)
              items
          }
        }
    )
    .mapConcat(items => items)
    .wireTap(data => println(s" After map Async${data}"))
    .mapAsync(1)(jsvalue => {
      //Get Repo
      getHttpRequest(Uri(s"https://api.github.com/repos/" +
        s"${(jsvalue \ "repository" \ "owner" \ "login").as[String]}/" +
        s"${(jsvalue \ "repository" \ "name").as[String]}/contents/" +
        s"${(jsvalue \ "path").as[String]}".replace(" ", "%20"))
      )
        .map { data => Json.parse(data) }
        .flatMap { repo =>
          println(s" Repo - ${repo}")
          //Get Raw
          getHttpRequest(Uri(s"${(repo \ "download_url").as[String]}"))
//            .onComplete {
//              case Success(raw) => raw.slice(raw.indexOfSlice("AWSAccessKeyId"), raw.indexOfSlice("AWSAccessKeyId") + 100)
//              case Failure(e) => println(s"Failure: $e")
//            }
            .map { raw =>
              //              println(s"Here is raw data: ${raw}")
              raw.slice(raw.indexOfSlice("AWSAccessKeyId"), raw.indexOfSlice("AWSAccessKeyId") + 100)
            }
        }
    }).buffer(256, OverflowStrategy.backpressure).async

  //  def apply() = delayBased
  def apply() = gitHubApiSource

  def getHttpRequest(uri: Uri) = {
    println(uri)
    Http().singleRequest(HttpRequest(
      method = HttpMethods.GET,
      uri = uri,
      headers = List(authorization))).flatMap {
      response =>
        println(response)
        Unmarshal(response).to[String]
    }
  }
}


