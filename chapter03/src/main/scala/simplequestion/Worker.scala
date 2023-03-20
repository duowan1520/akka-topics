package simplequestion

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

import scala.util.Random

object Worker {

  // worker 的协议也与前面的示例略有不同。
  sealed trait Command
  // Parse 消息不需要包含要打印的文本，现在它在创建时作为输入参数传递给 worker。
  final case class Parse(replyTo: ActorRef[Worker.Response])
      extends Command

  // Done 也不包含对要解析的文本的引用；在这里，ask 中使用的报告就是这样做的。
  sealed trait Response
  final case object Done extends Response

  def apply(text: String): Behavior[Command] =
    Behaviors.receive { (context, message) =>
      message match {
        // 创建时，它会收到必须处理的文件。
        // 在收到 Parse 以及它应该响应的引用之后，它开始解析文件。
        case Parse(replyTo) =>
          // 解析只是一个伪解析，其唯一目的是引入持续时间。完成后，它会响应 Done。
          fakeLengthyParsing(text)
          // 到目前为止，您已经了解了 actor 如何使用上下文记录、询问、生成和引用自己。当您想使用 actor 的路径时，使用 self 会很方便。
          // 您可能还记得上一章中，路径是由 actor 的父母和孩子的层次结构形成的。在下一个片段中，您有其中一个 worker 的路径。
          context.log.info(s"${context.self.path.name}: done")
          replyTo ! Worker.Done
          Behaviors.same
      }
    }

  private def fakeLengthyParsing(text: String): Unit = {
    // 这是一个假解析，需要两到四秒才能完成。这与管理器的 3 秒超时时间有关，因此平均有一半的请求超时。
    val endTime =
      System.currentTimeMillis + Random.between(2000, 4000)
    while (endTime > System.currentTimeMillis) {}
  }
}
