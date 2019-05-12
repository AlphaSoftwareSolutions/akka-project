package eu.isic.akka.data

import akka.actor.Actor
import eu.isic.akka.data.UserDatabase._
import eu.isic.akka.restserver.{DeliveryAdress, User}

object UserDatabase {

  sealed trait UserDatabaseResponse

  case class GetPassword(pwd: String)

  case class GetUser(id: String)

  case class Register(user: User)

  case class GetLoggedInUser(user: User)

  case class AddDeliveryAdress(user: String, deliveryAdress: DeliveryAdress)

  case class AdressContainer(adresses: List[DeliveryAdress])

  case class UserContainer(users: List[User])

  case class SelectDeliveryAdress(user: String)

  case object GetAllUsers

  case object Ok2 extends UserDatabaseResponse

  case object NotOk2 extends UserDatabaseResponse


}

class UserDatabase extends Actor {
  private var adressList = List.empty[DeliveryAdress]

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
      if (customer.isDefined) {
        customer.foreach { user =>
          this.sender() ! user.pwd
        }
      }
      else this.sender() ! " Not found"

    case SelectDeliveryAdress(user) =>
      if (adressList.isEmpty) {
        val loggedUser = User.USER_LIST.find(_.id == user)
        if (loggedUser.isDefined) {
          loggedUser.foreach { user =>
            user.adresses.foreach { adr =>
              adressList ::= adr
            }
          }
          this.sender() ! AdressContainer(adressList)

        } else this.sender() ! "Failure"
      }
      else this.sender() ! AdressContainer(adressList)

    case AddDeliveryAdress(user, a) =>
      val customer = User.USER_LIST.find(_.id == user)
      if (customer.isDefined) {
        adressList ::= a
        this.sender() ! Ok2
      }
      else this.sender() ! "Could not process new delivery Adress"


  }
}