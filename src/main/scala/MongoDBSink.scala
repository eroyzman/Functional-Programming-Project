//package edu.fp.examples.app.stages

import akka.NotUsed
import akka.stream.alpakka.mongodb.scaladsl.MongoSink
import akka.stream.scaladsl.{Flow, Sink}
import com.mongodb.reactivestreams.client.{MongoClients, MongoCollection}
//import edu.fp.examples.app.dto.Message
//import Message.Trade
import org.bson.Document

object MongoDBSink {
  val dbName = "test"
  val collectionName = "employee"

  val collection: MongoCollection[Document] = MongoClients.create().getDatabase(dbName).getCollection(collectionName)


  def apply(): Sink[String, Any] = Flow[String].map(m =>

    new Document()
      .append("keyValue", m.substring(0, m.indexOf("#")))
      .append("langType", m.substring(m.lastIndexOf("#") + 1))
  )
    .to(MongoSink.insertOne(collection))


}
