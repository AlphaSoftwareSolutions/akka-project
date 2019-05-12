package eu.isic.akka.data

import akka.actor.FSM
import eu.isic.akka.data.BasketDatabase._
import eu.isic.akka.restserver.{DeliveryAdress, Product, User}


object BasketDatabase {


  sealed trait BasketData{}
  case class BasketContainer(products: List[Product]) extends BasketData

  case class PaidContainer(paid: startPaymentContainer) extends BasketData

  case class startPaymentContainer(id: Int, basketList: List[Product], deliverList: DeliveryAdress) extends BasketData

  sealed trait BasketCommand{}

  case class AddToBasket(product: Product) extends BasketCommand
  case object GetBasketInformation extends BasketCommand
  case object StartPayment extends BasketCommand
  case object PaymentDone extends BasketCommand

  case class PaymentInProgressContainer(id: String, number: Int, products: List[Product]) extends BasketState
  sealed trait BasketState{}
  case object Paid extends BasketState
  case object PaymentInProgress extends BasketState
  case object Unpaid extends BasketState

  case class BasketInformation (amount: Int)

  sealed trait BasketResponse
  case object Ok1 extends BasketResponse

}


class BasketDatabase extends FSM[BasketState, BasketData] {
  private var orderId = 1
  private var deliveryAdr = List.empty[DeliveryAdress]
  //  private var basketItems = List.empty[BasketContainer]

  when(Unpaid) {
    case Event(AddToBasket(product), container: BasketContainer) =>
      stay() using container.copy(product :: container.products)
    //      goto(PaymentInProgress)
  }
  when(PaymentInProgress) {
    case Event(PaymentInProgressContainer(user, number, products), _) =>
      val customer = User.USER_LIST.find(_.id == user)
      if (customer.isDefined) {
        customer.foreach { cust =>
          cust.adresses.foreach { adre =>
            if (adre.id.equals(number))
              deliveryAdr ::= adre
            this.sender() ! startPaymentContainer(orderId, products, adre)
          }
        }
      }
      //      goto(Paid)
      stay()
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

}
