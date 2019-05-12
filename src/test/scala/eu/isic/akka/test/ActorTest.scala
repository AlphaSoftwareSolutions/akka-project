package eu.isic.akka.test

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{TestKit, TestProbe}
import eu.isic.akka.test.TestActor.GetInformation
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}


class TestActor extends Actor{
  override def receive: Receive = {
    case GetInformation=>
      this.sender() ! "Hello"
  }
}
object TestActor{
  case  object GetInformation
}
class ActorTest extends TestKit(ActorSystem()) with WordSpecLike with BeforeAndAfterAll{
  override def afterAll(): Unit = TestKit.shutdownActorSystem(system)
  "I want to send a message to an Actor and receive a response" in {
    val actorRef = system.actorOf(Props(classOf[TestActor ] ))
    val testProbe = TestProbe(  )
    testProbe.send(actorRef, GetInformation )
    testProbe.expectMsg("Hello")
  }
}
