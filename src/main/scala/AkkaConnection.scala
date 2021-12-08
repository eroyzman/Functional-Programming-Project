import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.unmarshalling.Unmarshal

import java.time.Instant
import akka.stream.scaladsl.Source
import play.api.libs.json._

import scala.concurrent.{Future, duration}
import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success}

object AkkaConnection extends App {

  implicit val system = ActorSystem(Behaviors.empty, "SingleRequest")
  // needed for the future flatMap/onComplete in the end
  implicit val executionContext = system.executionContext

  def resolveEvent(): Future[Seq[Any]] = Future {
    Seq()
  }

  Source.tick(FiniteDuration(0, duration.SECONDS), FiniteDuration(5, duration.SECONDS), 1)
  .mapAsync(1)(_ => resolveEvent())
    .statefulMapConcat(() => {
      var lastEventTimestamp: Instant = Instant.ofEpochMilli(0)
      {
        (sequence) =>
          val filtered = sequence.filter(i => isAfter(Instant))
          if (filtered.nonEmpty){
            lastEventTimestamp = filtered(0)
          }
          filtered
      }
    }

    )


  val authorization = headers.Authorization(BasicHttpCredentials("token", "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"))
  //  val result: Future[HttpResponse] = Http().singleRequest(HttpRequest(
  val result = Http().singleRequest(HttpRequest(
    method = HttpMethods.GET,
    uri = Uri("https://api.github.com/search/code") withQuery ("q", "addClass") +: ("in", "file") +: Query.Empty,
    headers = List(authorization)
  )
    ).flatMap { res =>
      Unmarshal(res).to[String].map { data =>
        Json.parse(data)
      }
    }

  result.onComplete {
    case Success(js) =>
      println(s"Success: $js")
    case Failure(e) =>
      println(s"Failure: $e")
  }
}
