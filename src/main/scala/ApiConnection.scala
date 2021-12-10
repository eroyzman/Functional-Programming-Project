import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri.Empty.withQuery
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.Source
import play.api.libs.json._

import scala.util.{Failure, Success}
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}
import scala.concurrent.duration.{DurationInt, FiniteDuration}

object ApiConnection extends App {

  implicit val system: ActorSystem[Nothing] = {
    ActorSystem(Behaviors.empty, "SingleRequest")
  }
  // needed for the future flatMap/onComplete in the end
  implicit val executionContext: ExecutionContextExecutor = {
    system.executionContext
  }
  val authorization = headers.Authorization(BasicHttpCredentials("token", "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"))

  val SEARCH: Uri = Uri("https://api.github.com/search/code") withQuery ("q", "AWSAccessKeyId") +: ("in", "file") +: Query.Empty

  apply("")

  def apply(value: String) = {

    Source.fromMaterializer((mat, a) => {
      println(s"Here $mat")
      implicit val context: ExecutionContext.parasitic.type = ExecutionContext.parasitic
      val (queue, source) = Source.queue[String](256, OverflowStrategy.dropHead).preMaterialize()(mat)

      Source.tick(0.second, 15.seconds, 1).mapAsync(1)(_ =>
        getHttpRequest(SEARCH).map { data => Json.parse(data) }
          .map(value => (value \ "items")
            .validate[Seq[JsValue]] match {
            case JsSuccess(items, _) =>
              items foreach {
                items => {
                  //Get Repo
                  getHttpRequest(Uri(s"https://api.github.com/repos/" +
                    s"${(items \ "repository" \ "owner" \ "login").as[String]}/" +
                    s"${(items \ "repository" \ "name").as[String]}/contents/" +
                    s"${(items \ "path").as[String]}".replace(" ", "%20"))).map { data => Json.parse(data) }
                    .onComplete {
                      case Success(repo) =>
                        //Get Raw
                        getHttpRequest(Uri(s"${(repo \ "download_url").as[String]}")).map { raw =>
                          println(raw.slice(raw.indexOfSlice("AWSAccessKeyId"), raw.indexOfSlice("AWSAccessKeyId") + 100))
                          queue.offer(raw.slice(raw.indexOfSlice("AWSAccessKeyId"), raw.indexOfSlice("AWSAccessKeyId") + 100))
                        }
                      case Failure(e) =>
                        println(s"Failure: $e")
                    }
                }
              }
            case error: JsError =>
              println(error)
          }
          )).run()

      source

    }).async

  }

  def getHttpRequest(uri: Uri) = {
    Http().singleRequest(HttpRequest(
      method = HttpMethods.GET,
      uri = uri,
      headers = List(authorization))).flatMap {
      response =>
        Unmarshal(response).to[String]
    }
  }
}

