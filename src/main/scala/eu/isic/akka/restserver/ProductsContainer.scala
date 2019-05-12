package eu.isic.akka.restserver

import akka.actor.{Actor, ActorSystem, FSM, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.Credentials
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import eu.isic.akka.data.BasketDatabase._
import eu.isic.akka.data.ModelJsonFormats
import eu.isic.akka.restserver.ProductDatabase.{Ok, _}
import eu.isic.akka.restserver.UserDatabase._

import scala.concurrent.Await
import scala.concurrent.duration.DurationDouble

case class Price(number: Double, currency: String)

case class Product(id: Int, name: String, price: Price, description: String, itemsLeft: Int)

case class Basket(id: Int)

case class DeliveryAdress(id: Int, street: String, postcode: Int, city: String)

case class User(id: String, pwd: String, name: String, surname: String, adresses: List[DeliveryAdress], bankAccount: String)


//object DeliveryAdress {
//  var DELIVERYADRESS_LIST = List.empty[DeliveryAdress]
//}

object User {
  var USER_LIST = List {
    User("edib.isic@gmail.com", "secret", "Edib", "Isic", (List(DeliveryAdress(1, "Lerchenauerstr.146a", 80935, "München"), (DeliveryAdress(2, "Feldmochingerstraße 212", 80995, "München")))), "DE5893329493249423949")
  }
}

//case class UserContainer(user: User)

//case class UsersContainer(users: List[User])

class UserDatabase extends Actor {
  override def receive: Receive = {
    case Register(user) =>
      val duplicateEmail = User.USER_LIST.find(_.id == user.id)
      if (duplicateEmail.isDefined) {
        this.sender() ! NotOk2
      } else {
        UserContainer(User.USER_LIST)
        User.USER_LIST ::= user
        this.sender() ! Ok2
      }
    case GetAllUsers =>
      this.sender() ! UserContainer(User.USER_LIST)

    case GetUser(user) =>
      val customer = User.USER_LIST.find(_.id == user)
      customer.foreach { user =>
        if (customer.isDefined) {
          this.sender() ! user
        }
        else this.sender() ! " Not found"
      }
    case GetPassword(id) =>
      val customer = User.USER_LIST.find(_.id == id)
      customer.foreach { user =>
        if (customer.isDefined) {
          this.sender() ! user.pwd
        }
        else this.sender() ! " Not found"
      }
    case AddDeliveryAdress(user, a) =>
      val customer = User.USER_LIST.find(_.id == user)
      customer.foreach { user =>
        if (customer.isDefined) {
          User.USER_LIST ::= a

        }
        //          user.adresses ::= a
        //        this.sender() ! Ok2

        else this.sender() ! "Could not process new delivery Adress"

      }

  }
}

object UserDatabase {

  case class GetPassword(pwd: String)

  case class GetUser(id: String)

  case class Register(user: User)

  case class GetLoggedInUser(user: User)

  case object GetAllUsers

  case class AddDeliveryAdress(user: String, deliveryAdress: DeliveryAdress)

  case class AdressContainer(adresses: List[DeliveryAdress])

  case class UserContainer(users: List[User])

  sealed trait UserDatabaseResponse

  case object Ok2 extends UserDatabaseResponse

  case object NotOk2 extends UserDatabaseResponse

}

//class Basket extends FSM[BasketState, BasketData]{
////  when(Unpaid){
////    case Event(AddTo, stateData)
////  }
//  startWith(Unpaid, BasketDataContainer(items = List.empty))
//}

//object Basket {
//  var BASKET_LIST = List.empty[Product]
//}


class BasketDatabase extends FSM[BasketState, BasketData] {
  //  private var basketList = List.empty[Product]
  private var adressList = List.empty[DeliveryAdress]
  private var orderId = 1

  when(Unpaid) {
    case Event(AddToBasket(product), container: BasketContainer) =>
      stay() using container.copy(product :: container.products)
    case Event(SelectDeliveryAdress(user), _) =>
      val loggedUser = User.USER_LIST.find(_.id == user)
      if (loggedUser.isDefined) loggedUser.foreach { user =>
        user.adresses.foreach { deliverAdress =>
          adressList ::= deliverAdress
        }
      }
      this.sender() ! AdressList(adressList)
      goto(PaymentInProgress)
  }
  when(PaymentInProgress) {
    case Event(PaymentDone, _) =>
      this.sender() ! (orderId += 1)
      goto(Paid)
  }
  when(Paid) {
    case _ => stay()
  }
  whenUnhandled {
    case Event(GetBasketInformation, container: BasketContainer) =>
      this.sender() ! BasketContainer(products = container.products)
      stay()
    case Event(msg, _) =>
      println(s"Cannot handle $msg in ${this.stateName}")
      stay()
  }
  onTransition {
    case x -> y =>
      println(s"Going from $x to $y")
  }

  startWith(Unpaid, BasketContainer(products = List.empty))

  //  override def receive: Receive = {
  ////    case AddToBasket123(p) =>
  //////      basketList ::= p
  ////      this.sender() ! Ok1
  //    case GetBasketInformation =>
  //      this.sender() ! BasketContainer(basketList)

  //    }
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

class ProductDatabase extends Actor {

  //  private var productList = List.empty[Product]

  override def receive = {
    case AddProduct(p) =>
      Product.PRODUCT_LIST ::= p
      this.sender() ! Ok
    case GetProducts =>
      this.sender() ! ProductContainer(Product.PRODUCT_LIST)
  }
}

object ProductDatabase {

  sealed trait ProductDatabaseCommand

  case class AddProduct(product: Product) extends ProductDatabaseCommand

  case class ProductContainer(products: List[Product])

  case object GetProducts extends ProductDatabaseCommand

  sealed trait ProductDatabaseResponse

  case object Ok extends ProductDatabaseResponse

}


object ActorApplication extends ModelJsonFormats {

  def main(args: Array[String]): Unit = {
    implicit val timeout = Timeout(5 seconds)

    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()
    implicit val ec = system.dispatcher


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
        //        Route.seal {
        //          pathPrefix("secured") {
        //            authenticateBasic(realm = "secure site", myUserPassAuthenticator) { user =>
        //              path(Segment) { segment =>
        //                var user = User.USER_LIST.find(_.id == segment)
        //                if (user.isDefined)
        //                  complete(s"Hi '$user' you logged in")
        //                else complete(s"login failed Please register at ${"users" / "register"}")
        //              }~ path("basketActor") {
        //                (pathEnd & get) {
        //                  implicit val timeout = Timeout(3 seconds)
        //                  onSuccess((basketActor ? GetBasketProducts).mapTo[BasketList]) { res =>
        //                    complete(BasketContainer(products = res.products))
        //                          }~ (path("add" / IntNumber) & post) { number =>
        //                            val product = Product.PRODUCT_LIST.find(_.id == number)
        //                            if (product.isDefined) {
        //                              product.foreach { product =>
        //                                basketActor ! AddProductToBasket(product)
        //                                //            complete(Basket.BASKET_LIST ::= product)
        //                              }
        //                              complete(product)
        //                            } else complete("Not found")
        //                          }
        //                }
        //              }
        //            }
        //          }
        //      }
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
                      basketActor ! AddToBasket(product)
                    }
                    complete(product)
                  } else complete("Product Not found. You have to enter a valid Product-ID ")
                }
              }, path("startPayment") {
                //selecting a delivery adress
                (pathEnd & get) {
                  implicit val timeout = Timeout(3 seconds)
                  onSuccess((basketActor ? SelectDeliveryAdress(user)).mapTo[AdressList]) { res =>
                    complete(AdressContainer(adresses = res.adresses))
                  }
                }
              },
              (path("addDeliveryAdress") & post) {
                entity(as[DeliveryAdress]) { a =>
                  implicit val timeout = Timeout(3 seconds)
                  onSuccess((userActor ? AddDeliveryAdress(user, a)).mapTo[UserDatabaseResponse]) {
                    case Ok2 => complete("New Delivery Adress Added")
                    case _ => complete("Failed")
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

