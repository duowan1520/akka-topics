package com.manning

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import scala.concurrent.duration.DurationInt

// 在并发环境中，从时间的角度思考很容易出错。
// Akka 简化了这个任务。您可以为它安排消息，而不是让 actor 等待 - 正如您可能从线程中了解到的那样。

// 让我们实现另一个与上一个非常相似的钱包。一切都一样，除了使用这个钱包你可以发送一条停用消息，让钱包停用几秒钟。
// 通过此停用，此钱包的用户不再需要记住发送激活消息以重新激活钱包。停用后，钱包将在几秒钟后向自身发送一条激活消息。

// 请记住，没有人在 actor 身边等待。 Actor 照常处理消息。在这个调度中没有什么比阻塞更好的了。
object WalletTimer {

  // 除了现在将持续时间作为参数的 Deactivate之外，Activate 被标记为私有，因此不可能从 actor 外部激活钱包。
  // 这个想法是钱包只能由 actor 自己重新激活。这个新变体称为 WalletTimer。

  // 将协议设为私有或公开是 Akka 中使用的常见模式。
  // 这允许您同时提供公共 API（一组其他 actor 可以使用的消息）和私有 API（处理 actor 的内部功能）。
  sealed trait Command
  final case class Increase(amount: Int) extends Command
  final case class Deactivate(seconds: Int) extends Command
  private final case object Activate extends Command

  def apply(): Behavior[Command] =
    activated(0)

  def activated(total: Int): Behavior[Command] = {
    // 要实现调度 Activate，您需要使用工厂 Behaviors.withTimers 来创建调度构建器。
    Behaviors.receive { (context, message) =>
      Behaviors.withTimers { timers =>
        message match {
          case Increase(amount) =>
            val current = total + amount
            context.log.info(s"increasing to $current")
            activated(current)
          case Deactivate(t) =>
            // 使用此构建器，您可以使用 .startSingleTimer 创建一个计时器，它会安排一次消息。
            // 这两个部分是一种行为，可以与其他行为（激活或停用）结合使用，就像其他任何行为一样

            // 它的签名期望消息被发送和延迟，如下所示。 T 是 actor 协议的类型，FiniteDuration 是延迟的类型。

            // 通过导入 scala.concurrent.duration._ 并在整数本身中调用 .second，可以将整数自动转换为持续时间，即 FiniteDuration
            // - 例如以秒为单位。这显示在以下清单中。
            timers.startSingleTimer(Activate, t.second)
            deactivated(total)
          case Activate =>
            Behaviors.same
        }
      }
    }
  }

  def deactivated(total: Int): Behavior[Command] = {
    Behaviors.receive { (context, message) =>
      message match {
        case Increase(_) =>
          context.log.info(s"wallet is deactivated. Can't increase")
          Behaviors.same
        case Deactivate(t) =>
          context.log.info(
            s"wallet is deactivated. Can't be deactivated again")
          Behaviors.same
        case Activate =>
          context.log.info(s"activating")
          activated(total)
      }
    }
  }
}
