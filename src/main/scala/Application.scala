import ApiSource.{delayBased, gitHubApiSource}
import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http

//import akka.http.scaladsl.client.RequestBuilding.WithTransformation
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives.{concat, get, getFromResource, handleWebSocketMessages, path}
import akka.stream.{ClosedShape, FlowShape, Materializer, UniformFanOutShape, scaladsl}
import akka.stream.scaladsl.{Broadcast, BroadcastHub, Flow, GraphDSL, Keep, Merge, RunnableGraph, Sink, Source, Unzip, Zip}
import akka.stream.scaladsl.GraphDSL.Implicits.fanOut2flow
//import GraphDSL.Implicits._
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule

import scala.concurrent.{ExecutionContextExecutor, Future}

object Application extends App {

  implicit val system: ActorSystem = ActorSystem("Demo-Basics")
  implicit val materializer: Materializer.type = Materializer
//  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  val mapper: ObjectMapper = new ObjectMapper().registerModule(DefaultScalaModule)

  private val graphSource = gitHubApiSource
    .via(GraphDSL.create() { implicit graphBuilder =>
      val IN = graphBuilder.add(Broadcast[String](2))
      val OUT = graphBuilder.add(Merge[String](1))
      IN ~> OUT
      IN ~> MongoDBSink()
      FlowShape(IN.in, OUT.out)
    })
//    .toMat(BroadcastHub.sink)(Keep.right)
//    .run

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
//                        handleWebSocketMessages(Flow.fromSinkAndSource(Sink.ignore, gitHubApiSource.map(TextMessage(_))))
            handleWebSocketMessages(Flow.fromSinkAndSource(Sink.ignore, graphSource.map(mapper.writeValueAsString(_)).map(TextMessage(_))))
//                        gitHubApiSource.to(MongoDBSink()).run()
//            //
//                        handleWebSocketMessages(
//                          Flow.apply[Message]
//                            .map(m => m.asTextMessage)
//                            .map(tm => s"Echo: ${tm.getStrictText}")
//                            .map(TextMessage(_))
//                        )
          }
        )
      }
    )
  }

}