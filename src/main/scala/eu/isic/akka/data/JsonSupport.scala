package eu.isic.akka.data

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import eu.isic.akka.data.BasketDatabase.{BasketContainer, PaidContainer, startPaymentContainer}
import eu.isic.akka.data.ProductDatabase.ProductContainer
import eu.isic.akka.data.UserDatabase.{AdressContainer, GetLoggedInUser, UserContainer}
import eu.isic.akka.restserver.{DeliveryAdress, Price, Product, User}
import spray.json.DefaultJsonProtocol

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {

  implicit val priceFormat = jsonFormat2(Price)
  implicit val productFormat = jsonFormat5(Product.apply)
  //  implicit val adressesFormat = jsonFormat1()
  implicit val deliveryAdressFormat = jsonFormat4(DeliveryAdress.apply)

  implicit val productContainerFormat = jsonFormat1(ProductContainer)
  implicit val basketContainerFormat = jsonFormat1(BasketContainer)

  implicit val adressesContainerFormat = jsonFormat1(AdressContainer)
  implicit val startPaymentFormat = jsonFormat3(startPaymentContainer)
  implicit val paidFormat = jsonFormat1(PaidContainer)


  implicit val userFormat = jsonFormat6(User.apply)
  implicit val userContainerForm = jsonFormat1(UserContainer)
  implicit val getLoggedInUserForm = jsonFormat1(GetLoggedInUser)

}
