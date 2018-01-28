package openqs

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives

import scala.io.StdIn
import akka.stream._
import spray.json.DefaultJsonProtocol
import scala.concurrent._
import ExecutionContext.Implicits.global

// domain model
final case class Item(name: String, id: Long)
final case class Order(items: List[Item])

// collect your json format instances into a support trait:
trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit def itemFormat = jsonFormat2(Item)
  implicit def orderFormat = jsonFormat1(Order) // contains List[Item]
}

object Web extends App with Directives with JsonSupport {

  implicit val system = ActorSystem("my-system")
  implicit val materializer = ActorMaterializer()

  val route = path("hello") {
      get {
        complete(HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          "<h1>Say hello to akka-http</h1>"))
      }
    } ~
    path("randomitem") {
      get {
        // will marshal Item to JSON
        complete(Item("thing", 42))
      }
    } ~
    path("saveitem") {
      post {
        // will unmarshal JSON to Item
        entity(as[Item]) { item =>
          println(s"Server saw Item : $item")
          complete(item)
        }
      }
  }

  val (host, port) = ("localhost", 8080)
  val bindingFuture = Http().bindAndHandle(route, host, port)

  bindingFuture.failed.foreach {
    case ex: Exception =>
      println(s"$ex Failed to bind to $host:$port!")
  }

  println("Server online at http://localhost:8080/.")
  StdIn.readLine() // let it run until user presses return
  bindingFuture.flatMap(serverBinding => serverBinding.unbind()) // trigger unbinding from the port
    .onComplete(_ => system.terminate()) // and shutdown when done
}