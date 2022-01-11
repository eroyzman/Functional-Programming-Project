import ApiSource.{delayBased, gitHubApiSource}
import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding.WithTransformation
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives.{getFromResource, handleWebSocketMessages}
import akka.http.scaladsl.server.Directives.{concat, get, getFromResource, handleWebSocketMessages, path}
import akka.io.Udp.SO.Broadcast
import akka.stream.{ClosedShape, FlowShape, Materializer}
import akka.stream.scaladsl.{Broadcast, BroadcastHub, Flow, GraphDSL, Keep, Merge, RunnableGraph, Sink, Source, Unzip, Zip}

import scala.concurrent.Future

object Application extends App {

  implicit val system: ActorSystem = ActorSystem("Demo-Basics")
  implicit val materializer: Materializer.type = Materializer

  val serverSource: Source[Http.IncomingConnection, Future[Http.ServerBinding]] =
    Http().newServerAt("localhost", 8080).connectionSource()

  serverSource.runForeach { connection => // foreach materializes the source
    println("Accepted new connection from " + connection.remoteAddress)
    // ... and then actually handle the connection
    connection.handleWith(
      get {
        concat(
          path("") {
            getFromResource("./ui/index.html")
          },
          path("main.js") {
            getFromResource("./ui/main.js")
          },
          path("stream") {
//            handleWebSocketMessages(Flow.fromSinkAndSource(Sink.ignore, delayBased.via(testPrintFlow).map(TextMessage(_))))
            handleWebSocketMessages(Flow.fromSinkAndSource(Sink.ignore, gitHubApiSource.map(TextMessage(_))))
//                          handleWebSocketMessages(
//                            Flow.apply[Message]
//                              .map(m => m.asTextMessage)
//                              .map(tm => s"Echo: ${tm.getStrictText}")
//                              .map(TextMessage(_))
//                          )
          }
        )
      }
    )
  }

}