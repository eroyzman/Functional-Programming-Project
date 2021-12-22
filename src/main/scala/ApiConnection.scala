import ApiConnection.system
//import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri.Empty.withQuery
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.{ActorMaterializer, Materializer, OverflowStrategy}
import akka.stream.scaladsl.{Sink, Source}
import play.api.libs.json._

import scala.util.{Failure, Success}
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import akka.actor.{ActorSystem, Cancellable}

object ApiConnection extends App {


  //  implicit val system: ActorSystem[Nothing] = {
  //    ActorSystem(Behaviors.empty, "SingleRequest")
  //  }
  implicit val system: ActorSystem = ActorSystem()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher
  implicit val materializer: Materializer.type = Materializer
  // needed for the future flatMap/onComplete in the end
  val authorization = headers.Authorization(BasicHttpCredentials("token", "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"))
  val SEARCH: Uri = Uri("https://api.github.com/search/code") withQuery ("q", "AWSAccessKeyId") +: ("in", "file") +: Query.Empty


  def apply() = {
    //        Source.tick(3.second, 15.seconds, HttpRequest(uri = SEARCH)).map(response => println(Unmarshal(response).to[String])).run() //.buffer(16, OverflowStrategy.fail)
    Source.tick(1.second, 15.seconds, 1)
      .wireTap(data => println(s" Before map Async${data}"))
      .mapAsync(1)(_ =>
        getHttpRequest(SEARCH)
          .map { data => Json.parse(data) }
          .map { value =>
            (value \ "items")
              .validate[Seq[JsValue]] match {
              case JsSuccess(items, _) =>
                println(items)
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
            //Get Raw
            getHttpRequest(Uri(s"${(repo \ "download_url").as[String]}"))
              .map { raw =>
                println(s"Here is raw data: ${raw}")
                raw.slice(raw.indexOfSlice("AWSAccessKeyId"), raw.indexOfSlice("AWSAccessKeyId") + 100)
              }
          }
      }) //.buffer(256, OverflowStrategy.backpressure).async
  }

  def getHttpRequest(uri: Uri) = {
    println("getHttpRequest")
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


