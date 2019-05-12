package eu.isic.akka.restserver

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.Credentials
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import eu.isic.akka.data.BasketDatabase._
import eu.isic.akka.data.ProductDatabase.{Ok, _}
import eu.isic.akka.data.UserDatabase._
import eu.isic.akka.data.{BasketDatabase, JsonSupport, ProductDatabase, UserDatabase}

import scala.concurrent.Await
import scala.concurrent.duration.DurationDouble

case class Price(number: Double, currency: String)

case class Product(id: Int, name: String, price: Price, description: String, itemsLeft: Int)

case class DeliveryAdress(id: Int, street: String, postcode: Int, city: String)

case class User(id: String, pwd: String, name: String, surname: String, adresses: List[DeliveryAdress], bankAccount: String)


object User {
  var USER_LIST = List {
    User("edib.isic@gmail.com", "secret", "Edib", "Isic", (List(DeliveryAdress(1, "Lerchenauerstr.146a", 80935, "München"), (DeliveryAdress(2, "Feldmochingerstraße 212", 80995, "München")))), "DE5893329493249423949")
  }
}

object Product {
  var PRODUCT_LIST = List(
    Product(1, "iPhone", Price(899.99, "EUR"), "The iPhone is a smartphone made by Apple that combines a computer, iPod, digital camera and cellular phone into one device with a touchscreen interface. The iPhone runs the iOS operating system (OS)", 50),
    Product(2, "MacBook", Price(2499.99, "EUR"), "MacBook is a term used for a brand of Mac notebook computers that Apple started producing in 2006.", 90),
    Product(3, "iPhone X", Price(999.99, "EUR"), "Das iPhone X ist ein Smartphone des US-amerikanischen Unternehmens Apple. Es wurde am 3. November 2017 auf den Markt gebracht.[5] Der herstellerseitige Verkauf wurde inzwischen eingestellt. ", 10),
    Product(4, "Samsung Galaxy S8", Price(489.99, "EUR"), "Samsung Exynos 8895 Octa ARM Mali-G71 MP20 4 GB Hauptspeicher, 64 GB UFS 2.1", 20),
    Product(5, "Huawei Mate 10 Pro", Price(340.99, "EUR"), "HiSilicon Kirin 970 ARM Mali-G72 MP12 6 GB Hauptspeicher, 128 GB UFS 2.1", 30)
  )
}

object ActorApplication extends JsonSupport {

  def main(args: Array[String]): Unit = {

    implicit val timeout = Timeout(5 seconds)

    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()
    implicit val ec = system.dispatcher

    var basketItems = List.empty[Product]
    val dbActor = system.actorOf(Props(classOf[ProductDatabase]))
    val basketActor = system.actorOf(Props(classOf[BasketDatabase]))
    val userActor = system.actorOf(Props(classOf[UserDatabase]))

    def myGetSecret(id: String): String = {
      implicit val timeout = Timeout(3 seconds)
      val pwd = (userActor ? GetPassword(id)).mapTo[String]
      val result = Await.result(pwd, 5 seconds)
      return result
    }

    def myUserPassAuthenticator(credentials: Credentials): Option[String] =

      credentials match {
        case p@Credentials.Provided(id)
          if p.verify(myGetSecret(id)) => Some(id)
        case _ => None
      }


    val route: Route = {
      pathPrefix("registerUser") {
        post {
          entity(as[User]) { p =>
            implicit val timeout = Timeout(3 seconds)
            onSuccess((userActor ? Register(p)).mapTo[UserDatabaseResponse]) {
              case Ok2 => complete(s"Ok User ${p.id} sucessfully registered")
              case _ => complete(s"The Email ${p.id} allready exists.")
            }
          }
        }

      } ~
        pathPrefix("products") {
          concat(
            (pathEnd & get) {
              implicit val timeout = Timeout(3 seconds)
              onSuccess((dbActor ? GetProducts).mapTo[ProductContainer]) { res =>
                complete(ProductContainer(products = res.products))
              }
            },
            (path(IntNumber) & get) { number =>
              val product = Product.PRODUCT_LIST.find(_.id == number)
              if (product.isDefined) complete(product) else complete("Not found")
            },
            (path("add") & post) {
              entity(as[Product]) { p =>
                implicit val timeout = Timeout(3 seconds)
                onSuccess((dbActor ? AddProduct(p)).mapTo[ProductDatabaseResponse]) {
                  case Ok => complete("Product Added")
                  case _ => complete("Failed")
                }
              }

            }
          )
        } ~
        pathPrefix("secured") {
          authenticateBasic(realm = "secure site", myUserPassAuthenticator) { user =>
            concat(
              (pathEnd | pathSingleSlash) {
                get {
                  complete(s"User $user Authenticated OK")
                }
              },

              path("basketActor") {
                (pathEnd & get) {
                  implicit val timeout = Timeout(3 seconds)
                  //                  (basketActor ? GetBasketInformation).foreach(println)
                  onSuccess((basketActor ? GetBasketInformation).mapTo[BasketContainer]) { res =>
                    complete(BasketContainer(products = res.products))
                  }
                }
              },
              path("addToBasket" / IntNumber) { number =>
                post {
                  implicit val timeout = Timeout(3 seconds)
                  val product = Product.PRODUCT_LIST.find(_.id == number)
                  if (product.isDefined) {
                    product.foreach { product =>
                      basketItems ::= product
                      basketActor ! AddToBasket(product)
                    }
                    complete(product)
                  } else complete("Product Not found. You have to enter a valid Product-ID ")
                }
              }, path("selectDeliveryAdress") {
                //selecting a delivery adress
                (pathEnd & get) {
                  implicit val timeout = Timeout(3 seconds)
                  onSuccess((userActor ? SelectDeliveryAdress(user)).mapTo[AdressContainer]) { res =>
                    complete(AdressContainer(adresses = res.adresses))
                  }
                }
              },
              path("paid" / IntNumber) { number =>
                //selecting a delivery adress
                (pathEnd & post) {
                  implicit val timeout = Timeout(3 seconds)
                  basketActor ! StartPayment
                  onSuccess((basketActor ? PaymentInProgressContainer(user, number, basketItems)).mapTo[startPaymentContainer]) { res =>
                    complete(PaidContainer(paid = res))
                  }
                }
              },

              (path("addDeliveryAdress") & post) {
                entity(as[DeliveryAdress]) { a =>
                  implicit val timeout = Timeout(3 seconds)
                  onSuccess((userActor ? AddDeliveryAdress(user, a)).mapTo[UserDatabaseResponse]) {
                    case Ok2 => complete(s"New ${a} Adress Added")
                    case _ => complete("Failed to Add Adress")
                  }
                }
              },
              path("user") {
                (pathEnd & get) {
                  implicit val timeout = Timeout(3 seconds)
                  onSuccess((userActor ? GetUser(user)).mapTo[User]) { res =>
                    complete(GetLoggedInUser(user = res))
                  }
                }
              }
            )
          }
        }
    }


    Http().bindAndHandle(route, "0.0.0.0", 8083).foreach(binding =>
      println("Listening on 8083")
    )
  }

}

