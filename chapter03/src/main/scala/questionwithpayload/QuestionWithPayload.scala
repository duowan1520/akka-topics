package questionwithpayload

import akka.actor.typed.ActorSystem

/**
 * 总结：
 * 1）传统的 Akka 应用程序构建为 actor 的层次结构。第一个 actor 是 ActorSystem，下面是它的孩子。由 ActorSystem 创建的孩子可以依次创建其他孩子，依此类推，以这种方式创建 actor 的层次结构。
 * 2）当您在两个 actor 之间来回发送消息时，您需要适配器将响应转换为接收 actor 的协议。
 * 3）您可以使用 Context.messageAdapter 创建适配器。
 * 4）actor 可以 ask，而不是 tell (发后即忘)。这意味着 ask 期望在特定时间段内得到答案。因此，它必须指定超时和回调处理这两种情况，可能的成功和失败。
 * 5）你可以在像 sum(Int: a, Int: b) by sum(Int: a)(Int: b) 这样的函数中使用柯里化。这种柯里化允许您创建更复杂的适配器，您可以在 ask 时使用它们。
 */
object QuestionWithPayload extends App {

  val guardian: ActorSystem[Guardian.Command] =
    ActorSystem(Guardian(), "LoadedQuestion")
  guardian ! Guardian.Start

  println("press ENTER to terminate")
  scala.io.StdIn.readLine()
  guardian.terminate()
}
