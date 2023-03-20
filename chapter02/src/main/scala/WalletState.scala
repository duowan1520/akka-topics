package com.manning

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

object WalletState {

  // 您已经知道什么是协议。但是使用整数作为协议的参与者仅用于示例目的。
  // 任何真实的用例协议都是用几种类型实现的，这些类型属于一个层次结构，顶部有一个密封的 trait 。

  // Traits 就像 Java 中的接口。有关更多信息，请访问 https://docs.scala-lang.org/tour/traits.html。
  // 密封 trait 是一种允许编译器检查您的模式匹配是否详尽的特征。
  // 当它不详尽时，编译器会发出警告。有关更多信息，请访问 https://docs.scala-lang.org/tour/pattern-matching.html#sealed-classes。
  // 具体 sealed 类型。

  // 而不是使用 Behavior[Int] - 就像在以前的钱包中一样 - 钱包现在有一个 Behavior[Command]
  // 它的协议有两种类型的消息，Increase(amount: Int) 和 Decrease(amount: Int)，它们都属于 Command trait，如您在以下代码片段中所见。

  // 请注意协议是如何在 actor 中编写的。
  // 协议的消息总是特定于一个 actor，从这个意义上说，它们并不意味着被其他 actor 重用。它们是 actor 的 API，属于它。
  sealed trait Command
  final case class Increase(amount: Int) extends Command
  final case class Decrease(amount: Int) extends Command

  // 您可能想知道为什么协议类型现在称为 Command。这是一个常见的命名方式，可以很容易地看出哪些消息应该请求 actor 采取行动。
  // 并非所有消息都旨在向 actor 发出命令，在下一章中您将看到这方面的示例。

  // 要在 actor 中存储消息数和最大值，您现在需要学习一些新知识 — 如何在向 actor 传递初始值的同时创建它。
  // 在前面的示例中，您创建了一个 actor，但在创建过程中没有传递值。
  // 首先，您需要将这些变量——初始总数和最大值——添加到它的构造函数中，也就是添加到 wallet actor 的 def apply 方法中。
  // 严格来说它们不是变量，在 Scala 中它们被称为值；即不可变变量。添加这些“变量”应该是这样的。

  // 其次，您必须从一次调用到下一次调用，从一条消息到下一条消息，保持两个参数的值。
  def apply(total: Int, max: Int): Behavior[Command] = {
    // 状态可以通过行为来实现，也就是函数。
    //
    Behaviors.receive { (context, message) =>
      message match {
        case Increase(amount) =>
          val current = total + amount
          if (current <= max) {
            context.log.info(s"increasing to $current")
            // 并以这样的方式进行，即在第一次增量之后，当下一个增量消息到达时，总数和最大值可用
            // 为此，您需要以 Behaviors.same 以外的方式引用相同的行为。

            // 因为您需要保留这两个值，所以您需要直接调用 apply(total, max) 并传递新的更新值。以下代码段显示了这一点。
            // 因为行为是函数，所以您可以使用它们的输入和输出将状态从一个函数转移到下一个函数，从一个行为转移到下一个行为。

            // 在这里，通过在 def apply(total: Int, max: Int) 的末尾设置 apply(total, max)，
            //   您可以使用相同的 apply 函数处理下一条消息，但使用新更新的输入参数。

            // 第一次将 total 和 max 设置为 0 和 2，使用 .apply(0,2)，即 WalletState(0,2)，感谢 Scala 语法糖。
            // 但是在处理完一条 Increase 消息后，下一个行为将是 .apply(1,2)。
            // 这样，行为就有了新的输入值，并且状态从一个消息转移到下一个消息。

            // 请记住，apply() 不是递归函数，而是一个接收函数作为输入并输出 Behavior 的工厂，即输出一个函数。
            apply(current, max)
          } else {
            context.log.info(
              s"I'm overloaded. Counting '$current' while max is '$max. Stopping")
            // 最后，当达到最大值时，actor 通过将下一个行为设置为 Behaviors.stopped 来停止。
            // 一旦 actor 停止，它就不能再处理任何消息。
            //  系统将其队列中剩余的所有消息或发送到其地址的新消息转发给另一个名为 deadLetters 的 actor。下一章将详细介绍这一点。
            Behaviors.stopped
          }
        case Decrease(amount) =>
          val current = total - amount
          if (current < 0) {
            context.log.info("Can't run below zero. Stopping.")
            Behaviors.stopped
          } else {
            context.log.info(s"decreasing to $current")

            // 定义 apply 方法的工厂 Behaviors.receive 接收类型为“(context, message) => Behavior”的函数，因此输出一个 Behavior。
            // 这是函数式风格，这里没有递归。

            // 记住：当钱包增加且当前值小于最大值时，通过设置 WalletState.apply(current, max) 作为下一个行为进一步增加。
            // 从第一条消息到第二条消息，行为先是.apply(0,2) 然后是.apply(0 + 1, 2)
            apply(current, max)
          }
      }
    }
  }
}
