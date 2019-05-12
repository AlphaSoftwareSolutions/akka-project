package eu.isic.akka.data

import eu.isic.akka.restserver.{DeliveryAdress, Product}


object BasketDatabase {


  sealed trait BasketData{}
  case class BasketContainer(products: List[Product]) extends BasketData
  case class AdressList(adresses: List[DeliveryAdress])

  sealed trait BasketCommand{}
  case class AddToBasket(product: Product) extends BasketCommand
  case object GetBasketInformation extends BasketCommand
  case object StartPayment extends BasketCommand
  case class SelectDeliveryAdress(user: String) extends BasketCommand

  case object PaymentDone extends BasketCommand

  sealed trait BasketState{}
  case object Paid extends BasketState
  case object PaymentInProgress extends BasketState
  case object Unpaid extends BasketState

  case class BasketInformation (amount: Int)

  sealed trait BasketResponse
  case object Ok1 extends BasketResponse

}
