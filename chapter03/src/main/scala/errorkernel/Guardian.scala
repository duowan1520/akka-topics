package errorkernel

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

// 显示 Guardian 如何创建 manager 并将任务委托给它。
object Guardian {

  sealed trait Command
  final case class Start(texts: List[String]) extends Command

  def apply(): Behavior[Command] = {
    // 这个工厂创建了一个只执行一次的行为——当 actor 被实例化时。
    // 它从只有一个输入参数的函数创建一个行为，上下文：ActorContext 用于生成 Manager。
    Behaviors.setup { context =>
      context.log.info("Setting up. Creating manager")
      // 该签名与 ActorSystem 的签名相同，并且还创建了一个您可以向其发送消息的 ActorRef。生成在 Behaviors.setup 中的以下示例中完成。
      val manager: ActorRef[Manager.Command] =
        context.spawn(Manager(), "manager-alpha")

      // 一旦 Guardian setup 了，它就会使用 manager 来委托解析 Start 消息中的文本。
      Behaviors.receiveMessage {
        // 当监护人收到 Start 时，它获取文本列表并将它们传递给 manager
        case Start(texts) =>
          manager ! Manager.Delegate(texts)
          Behaviors.same
      }
    }
  }
}
