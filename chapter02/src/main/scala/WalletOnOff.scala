package com.manning

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

// 在 actor 中保持具有不同行为的状态与使用变量存储状态具有不同的目的。您使用行为来表示 actor 对相同消息的不同反应。
// 如果你只想改变 actor 内部的变量，你不会使用这个方法，但是你在上一节中学到的东西。

// 根据 actor 的状态，相同的消息可以做非常不同的事情。例如，您可以让一个 actor 充当代理并具有两个状态。
// 打开和关闭。当它打开时，它会将消息转发到另一个目的地，就像任何代理一样。但是，当它关闭时，它不会转发任何消息。它通知第三方。

// 现在让我们实现另一个钱包。与之前的变体类似，您可以增加数量，但不能减少数量。只是为了简单起见。
// 新的是这个钱包有两种状态，用两种行为来表示。激活和停用。如果它被激活，它会像前面的例子一样响应。收到增量时增加总量。
// 另一方面，如果钱包被停用，则无法更改总金额。如果 actor 收到此状态的增加，它会丢弃它并报告未进行任何更改。

// 注：此功能使该 actor 成为有限状态机 (FSM)，因为它对相同输入的反应不同。无需过多赘述，FSM 是一种具有有限数量状态的模型。
object WalletOnOff {

  // 为简单起见，钱包现在既没有最大值也没有减少的能力，并且它有一个默认值零作为初始总数。有以下协议可用。
  sealed trait Command
  final case class Increase(amount: Int) extends Command
  final case object Deactivate extends Command
  final case object Activate extends Command

  // 实例化钱包不需要输入参数，因为它没有最大值，默认初始金额为零。您可以实例化它并在停用和激活这两种状态下发送 Increase 消息。
  // 要实现钱包的默认值为零并且其状态默认激活，您可以执行以下操作。
  def apply(): Behavior[Command] =
    activated(0)

  // Activated 是在处理 Increment 消息时增加总数并在处理 Deactivate 时切换到 deactivated behavior 。
  def activated(total: Int): Behavior[Command] =
    Behaviors.receive { (context, message) =>
      message match {
        case Increase(amount) =>
          val current = total + amount
          context.log.info(s"increasing to $current")
          activated(current)
        case Deactivate =>
          deactivated(total)
        case Activate =>
          // 您可能想知道为什么 Activate 上的钱包使用 Behavior.same 而不是 activated(total)。
          // 您可以使用 Behavior.same 因为没有递增，所以它实际上是再次使用相同的行为。
          // 您不需要在此处传递新的总数。您可以传递相同的总数，但 Behavior.same 更容易阅读。
          Behaviors.same
      }
    }

  // 最后，deactivated 行为的工作方式如下。它在收到 Increase 消息时记录无法增加，并在收到 Activate 消息时返回激活状态。
  // 收到 Deactivate 消息时，总数保持不变。
  def deactivated(total: Int): Behavior[Command] = {
    Behaviors.receive { (context, message) =>
      message match {
        case Increase(_) =>
          context.log.info(s"wallet is deactivated. Can't increase")
          Behaviors.same
        case Deactivate =>
          Behaviors.same
        case Activate =>
          context.log.info(s"activating")
          activated(total)
      }
    }
  }

  // 在本节中，您了解了如何在 Akka Typed 中创建 FSM。 Akka 的原语已经具有 FSM 的语义。
  // 您可以说 Actor 模型的公理已经包含这些语义。
  // 您在第一章中看到了这个公理，“一个 actor 可以指定用于它收到的下一条消息的行为”。
  // 因为这是直接在 Akka 中实现的，所以您可以只使用其基本构建块来创建 FSM。它的行为。
}
