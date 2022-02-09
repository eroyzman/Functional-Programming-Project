import Application.system
import Application.system.dispatcher
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Sink, Source}
import play.api.libs.json.{JsError, JsResult, JsSuccess, JsValue, Json}
import scala.concurrent.ExecutionContext
import java.nio.file.Paths

import scala.concurrent.Future
//import scala.concurrent.duration.{DurationInt, FiniteDuration}
import akka.actor.{ActorSystem, Cancellable}

import scala.concurrent.duration._

object ApiSource {
  //  implicit val context: ExecutionContext.parasitic.type = ExecutionContext.parasitic
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

  val gitHubApiSource: Source[String, Cancellable] = Source.tick(1.second, 20.seconds, 1)
    //    .wireTap(data => println(s" Before map Async${data}"))
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
          //println(s" Repo - ${repo}")
          //Get Raw
          val fileUrl: JsResult[String] = (repo \ "download_url").validate[String]

          fileUrl match {
            case s: JsSuccess[String] => getHttpRequest(Uri(s.value))
              .map { raw =>
                try {
                  val result = raw.slice(raw.indexOfSlice("AWSAccessKeyId"), raw.indexOfSlice("AWSAccessKeyId") + 100)
                  val extension = s.value.substring(s.value.lastIndexOf(".") + 1)
                  result.substring(result.indexOf("=") + 1, result.indexOf("&")) + "#" + extension
                } catch {
                  case e: Exception => e.toString
                }
              }
            case e: JsError => Future {
              "Errors: " + JsError.toJson(e).toString()
            }
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


