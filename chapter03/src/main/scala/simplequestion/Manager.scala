package simplequestion

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.util.Timeout

import scala.concurrent.duration.SECONDS
import scala.util.Failure
import scala.util.Success

// 下面代码展示了 manager 如何请求 worker 解析并等待响应。如果响应足够快，manager 会收到成功响应，否则会收到失败异常。
// 如果您查看解析某个错误时，您会发现发生了三件相关的事情。
// 1) manager 不再等待响应，而是登记失败，并报告任务未完成。
// 2) 在 worker 端，解析花费的时间超过了 manager 愿意等待的时间，
//    但是 worker 完成了任务并记录了它完成解析的事实。这就是您看到以下消息的原因。
// 3) 第三点是上一章提到的deadLetters actor。当 worker 完成任务时，它会尝试发回响应，但此消息永远不会到达 manager。
//    发生这种情况是因为请求通过了一个中间角色，比如前面示例中的适配器。
//    a) 创建了一个临时 actor，作为 manager 和实际负责超时的 worker 之间的代理。
//       当时间到了，它会向 manager 发送一条错误消息并停止。
//       因为它已经停止了，从 worker 到 proxy 的消息再也无法到达它。
//       然后 Actor 系统接管，发送给这个停止的 actor 的消息被转发给 deadLetters actor。
//       向 deadLetters actor 发送消息只保证尽力而为；也就是说，交付保证最多是一次。
//       这意味着每条消息只发送一次。它是否到达目的地对发送者来说无关紧要，它不等待确认。
// 如果由于某种原因 worker 出现问题并抛出异常，manager 只会收到超时。
// 在这种情况下，由于异常，worker 没有发送任何响应，因此死信中没有任何内容。
object Manager {

  // Manager 具有与前一个示例相同的协议，但它现在具有 Report 而不是适配器，它与 ask 结合使用。
  sealed trait Command
  final case class Delegate(texts: List[String]) extends Command
  // 报告是必要的处理 worker 的响应。
  // 在 ask 的回调中，manager 要给自己发消息，这跟 ask 的签名有关系。
  // 在你处理 ask 的签名之前，让我们解释一下这两个协议，Manager 的和 Worker 的，这样你就可以看到通信的双方。
  private final case class Report(description: String) extends Command

  def apply(): Behavior[Command] =
    Behaviors.setup { context =>
      // 此代码段需要一个超时时间，该超时时间指定 manager 将等待响应的时间。这是用超时设置的，如此处所示。
      implicit val timeout: Timeout = Timeout(3, SECONDS)

      Behaviors.receiveMessage { message =>
        message match {
          case Delegate(texts) =>
            // 使用收到的文本列表，它为每个文本创建一个 worker 并要求它进行解析。
            // 在询问中，它指定如何处理响应。
            // a) 如果成功，它会向自己发送一条包含 worker 姓名的报告消息。
            // b) 在失败时，它将异常消息包装在报告中，然后再次发送给自己。
            // c) 当收到报告时，无论是成功还是失败，都会打印出内容。
            texts.map { text =>
              val worker: ActorRef[Worker.Command] =
                context.spawn(Worker(text), s"worker-$text")
              // 在下面的代码片段中，worker 是在请求之前创建的。然后 ask 使用 worker 引用和 Worker.Parse 发送请求。
              // 最后你有回调来处理成功和失败的响应。根据它收到的响应，它会创建一份或另一份报告，具体取决于它是成功还是失败。
              // 这个隐式值被 Scala 传递给 ask，因为 ask 有一个这种类型的隐式参数。正如您在以下代码片段中的签名简化版本中所见。
              // 重复一下，Worker.Parse 可以在 manager 中用作 context.ask(worker, Worker.Parse)。
              // 即使签名是 Worker.Parse(replyTo: ActorRef[Worker.Response])，在实例化 Worker.Parse 时也不需要添加参数 replyTo。
              // 这是由 Actor 系统为您创建和添加的，充当中间 actor，就像上一节中的适配器一样。

              // 之前，您了解到当您将 ask 与 Parse(replyTo: ActorRef[Worker.Response]) 一起使用时，会为您添加 replyTo。
              // 要了解为什么会发生这种情况，您可以看一下 ask 的签名。以下代码段向您展示了此签名。
              // 上一节 context.ask(worker, Worker.Parse) 的例子中 worker 对应的是 target 参数，Worker.Parse 对应的是 createRequest。
              // 方法开头的注释 [Req, Res] 是该方法中类型的占位符。
              // 这样，例如对于 Req，编译器可以检查您在目标 RecipientRef[Req] 中使用的类型是否与 createRequest 中的类型相同：ActorRef[Res] => Req。
              // 在这个例子中,前面的 Req 是 Worker.Command。
              // Worker.Parse 可能看起来像一个对象，但正如您刚才看到的，createRequest 是一个带有签名 ActorRef[Res] => Req 的函数，而不是一个对象
              // 那么问题是 Worker.Parse 类是如何作为函数使用的呢？您在上一节中对此有所暗示，但让我们更明确一点。
              // 如果您不熟悉 Scala，这可能会让您感到困惑。如果类和对象具有 apply 方法，则它们可以像函数一样对待，而像 Worker.Parse 这样的案例类在幕后具有该 apply 方法。
              // 感谢编译器，案例类 Worker.Parse(replyTo: ActorRef[Worker.Response]) 创建了如下所示的方法。
              // def apply(replyTo: ActorRef[Worker.Response]): Worker.Parse = new Worker.Parse(replyTo)
              // 因为这个方法可用，编译器现在明白这就是你把 Worker.Parse 放在 ask 方法中的意思。
              // 编译器得出结论，您指的是此案例类的 apply 函数，因为这是在 createRequest 签名的上下文中唯一有意义的事情。
              // 就是这样：感谢编译器的语法糖和聪明，你通过使用 Worker.Parse 将 Worker.Parse.apply() 传递给这个方法，它等同于 ActorRef[Worker.Response]) => Worker.Parse。

              context.ask(worker, Worker.Parse) {
                // 包裹在 Success 或 Failure 中——回调产生一个 Manager.Command。此命令是回调发送回 Manager 的消息，这里是一个报告。
                case Success(Worker.Done) =>
                  Report(s"$text read by ${worker.path.name}")
                case Failure(ex) =>
                  Report(
                    s"parsing '$text' has failed with [${ex.getMessage()}")
              }
            }
            Behaviors.same
          case Report(description) =>
            context.log.info(description)
            Behaviors.same
        }
      }
    }
}
