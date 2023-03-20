package errorkernel

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

// 一旦 Guardian 和 Manager 一起被创建，它就可以开始处理消息了。这些由 Behaviors.receiveMessage 方法处理，
// 其中 Guardian 将 Start 中包含的任务委托给 Manager。
object Manager {

  sealed trait Command
  final case class Delegate(texts: List[String]) extends Command
  // 值得注意的是修饰符是私有的。这是为了确保除 Manager 以外没有其他 actor 使用该消息。
  // 否则，Manager 可以从任何确认文本已被解析的 actor 那里收到此消息。
  // 只有 Manager 可以将 Worker.Response 消息转换为 WorkerDoneAdapter 消息。
  private final case class WorkerDoneAdapter(
      response: Worker.Response)
      extends Command

  def apply(): Behavior[Command] =
    // 您应该在 Behaviors.setup 中创建适配器函数，这样它只会被创建一次
    Behaviors.setup { context =>
      // 要让 Manager 理解这一点，您需要做两件事。适配器函数和适配器消息。
      // 适配器函数用于向 worker 发送消息，adapter 消息用于接收 worker 发来的消息。
      val adapter: ActorRef[Worker.Response] =
        context.messageAdapter(response =>
          WorkerDoneAdapter(response))

      // manager 向 adapter 发送请求，adapter 向worker 发送请求，worker 响应 adapter，
      // adapter 最终将 worker 的消息包装成 WorkerDoneAdapter 发送给 manager。
      Behaviors.receiveMessage { message =>
        message match {
          case Delegate(texts) =>
            // Manager 拆分列表并为每个 worker 分配一份。
            // Manager 负责为每个文本创建一个 Worker 并将要解析的文本发送给该 Worker。
            texts.map { text =>
              val worker: ActorRef[Worker.Command] =
                context.spawn(Worker(), s"worker$text")
              context.log.info(s"sending text '${text}' to worker")
              worker ! Worker.Parse(adapter, text)
            }
            Behaviors.same
          case WorkerDoneAdapter(Worker.Done(text)) =>
            // Worker 的响应需要一个适配器，因为它的响应类型属于 Worker 而不属于 Manager。
            // worker 的响应由中间适配器函数转换为适配器消息。管理器收到包装在适配器消息中的 Worker.Done 并在其中查看以将解析的文本打印到控制台。
            context.log.info(s"text '$text' has been finished")
            Behaviors.same
        }
      }
    }
}
