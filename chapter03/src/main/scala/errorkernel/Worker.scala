package errorkernel

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

object Worker {

  sealed trait Command
  final case class Parse(
      replyTo: ActorRef[Worker.Response],
      text: String)
      extends Command

  sealed trait Response
  final case class Done(text: String) extends Response

  def apply(): Behavior[Command] =
    Behaviors.receive { (context, message) =>
      message match {
        case Parse(replyTo, text) =>
          // // 它将它们包装在 Manager 可以理解的委托消息中，并用 tell 发送它们，即 !。
          val parsed = naiveParsing(text)
          context.log.info(
            s"'${context.self}' DONE!. Parsed result: $parsed")
          // worker 使用自己的协议来响应，这是一个问题。请记住，每个 actor 都有自己的协议，这是它唯一理解的协议。
          // 因此，原则上， Manager 不能理解 Worker 的回答。
          replyTo ! Worker.Done(text)
          Behaviors.stopped
      }
    }

  def naiveParsing(text: String): String =
    text.replaceAll("-", "")

}
