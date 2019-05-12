package eu.isic.akka.data

import akka.actor.Actor
import eu.isic.akka.data.ProductDatabase.{AddProduct, GetProducts, Ok, ProductContainer}
import eu.isic.akka.restserver.Product

object ProductDatabase {

  sealed trait ProductDatabaseCommand

  sealed trait ProductDatabaseResponse

  case class AddProduct(product: Product) extends ProductDatabaseCommand

  case class ProductContainer(products: List[Product])

  case object GetProducts extends ProductDatabaseCommand

  case object Ok extends ProductDatabaseResponse

}

class ProductDatabase extends Actor {
  override def receive = {
    case AddProduct(p) =>
      Product.PRODUCT_LIST ::= p
      this.sender() ! Ok
    case GetProducts =>
      this.sender() ! ProductContainer(Product.PRODUCT_LIST)
  }
}