
//package edu.fp.examples.app

import akka.NotUsed
import akka.actor.{ActorSystem, Cancellable}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.TextMessage
import akka.http.scaladsl.server.Directives.{concat, get, getFromResource, handleWebSocketMessages, path}
import akka.http.scaladsl.server.PathMatcher
import akka.protobufv3.internal.ByteString
import akka.stream.{FlowShape, Materializer}
import akka.stream.scaladsl.GraphDSL.Implicits.fanOut2flow
import akka.stream.scaladsl.{Broadcast, BroadcastHub, Flow, GraphDSL, Keep, Merge, Sink, Source}
import com.fasterxml.jackson.databind.ObjectMapper
import play.api.libs.json.JsValue
//import com.fasterxml.jackson.databind.ObjectMapper
//import com.fasterxml.jackson.module.scala.DefaultScalaModule
//import edu.fp.examples.app.dto.Message
//import edu.fp.examples.app.integration.CryptoCompareSource
//import edu.fp.examples.app.stages.{MongoDBSink, PriceAvgFlow, PriceFlow, TradeFlow}
import akka.http.scaladsl.model.ws.Message
import akka.actor.typed.ActorRef
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.typed.scaladsl.ActorSink

import scala.concurrent.{ExecutionContextExecutor, Future}


object Application extends App {
  //  implicit val system: ActorSystem = ActorSystem()
  //  implicit val executionContext: ExecutionContextExecutor = system.dispatcher
  //  implicit val materializer: Materializer.type = Materializer


  //  val mapper: ObjectMapper = new ObjectMapper().registerModule(DefaultScalaModule)
  //
  //  private val graphSource = CryptoCompareSource(Seq("5~CCCAGG~BTC~USD", "0~Coinbase~BTC~USD", "0~Cexio~BTC~USD"))
  //private val graphSource: Source[String, Cancellable] = ApiConnection()
  //    .via(GraphDSL.create() { implicit graphBuilder =>
  //      val IN = graphBuilder.add(Broadcast[Map[String, Any]](2))
  //      val PRICE = graphBuilder.add(Broadcast[Message[Float]](2))
  //      val TRADE = graphBuilder.add(Broadcast[Message[Message.Trade]](2))
  //      val OUT = graphBuilder.add(Merge[Message[Any]](3))
  //
  //      IN ~> PriceFlow() ~> PRICE ~> PriceAvgFlow() ~> OUT
  //      PRICE                   ~> OUT
  //      IN ~> TradeFlow() ~> TRADE                   ~> OUT
  //      TRADE ~> MongoDBSink()
  //
  //      FlowShape(IN.in, OUT.out)
  //    })
  //    .toMat(BroadcastHub.sink)(Keep.right)
  //    .run

  //  trait Ack
  //  object Ack extends Ack
  //
  //  trait Protocol
  //  case class Init(ackTo: ActorRef[Ack]) extends Protocol
  //  case class Message(ackTo: ActorRef[Ack], msg: String) extends Protocol
  //  case object Complete extends Protocol
  //  case class Fail(ex: Throwable) extends Protocol
  //
  //  def targetActor() = ???
  //
  //  val actor: ActorRef[Protocol] = targetActor()
  //
  //  val sink: Sink[String, NotUsed] = ActorSink.actorRefWithBackpressure(
  //    ref = actor,
  //    messageAdapter = (responseActorRef: ActorRef[Ack], element) => Message(responseActorRef, element),
  //    onInitMessage = (responseActorRef: ActorRef[Ack]) => Init(responseActorRef),
  //    ackMessage = Ack,
  //    onCompleteMessage = Complete,
  //    onFailureMessage = (exception) => Fail(exception))

  implicit val system: ActorSystem = ActorSystem()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher
  implicit val materializer: Materializer.type = Materializer

  private val graphSource: Source[String, Cancellable] = ApiConnection()

  val serverSource: Source[Http.IncomingConnection, Future[Http.ServerBinding]] =
    Http().newServerAt("localhost", 8080).connectionSource()


  val bindingFuture =
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
              //                     graphSource.runWith(Sink.ignore)
              handleWebSocketMessages(Flow.fromSinkAndSource(Sink.ignore, graphSource.map(TextMessage(_))))
              //                     handleWebSocketMessages(
              //                       Flow.apply[Message]
              //                         .map(m => m.asTextMessage)
              //                         .map(tm => s"Echo: ${tm.getStrictText}")
              //                         .map(TextMessage(_))
              //                     )
            }
          )
        }
      )
    }

}
