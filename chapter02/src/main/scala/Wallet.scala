package com.manning

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.Behavior

object Wallet {

  // Akka API 使用 Behavior[Int] 指定钱包 actor 的行为使其可以接收 Int 类型的消息，如您在以下代码段中所见。
  def apply(): Behavior[Int] =
    // 要创建行为，您可以使用行为工厂，其中之一是 Behaviors.receive。该工厂是一个具有两个输入（上下文和消息）的函数，它创建一个行为作为输出。
    // 这些是 actor 的上下文——ActorContext——以及它接收到的消息。这里是一个 Int。这是签名。
    Behaviors.receive { (context, message) =>
      // 在这个工厂中，您现在可以使用消息和上下文；例如，记录收到的消息。为此，您可以使用上下文，因为上下文附带的功能之一是日志记录。
      // 您可以添加如下内容。
      context.log.info(s"received '$message' dollar(s)")

      // 最后，您需要设置下一条消息的行为。你可以选择相同的行为，为此你有另一个行为工厂，你用 Behavior.same 调用它。
      Behaviors.same
    }

}
