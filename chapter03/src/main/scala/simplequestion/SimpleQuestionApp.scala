package simplequestion

import akka.actor.typed.ActorSystem

// 在 Akka 中，ask 的意思是发送一个包含一定时间内回调的消息。actor 提出要求后，可能会发生两件事。
// 1）一定时间后，它要么收到指示消息已处理的响应，要么没有收到任何响应。这是时间限制：
// 2）如果在等待一定时间后没有得到响应，actor 必须决定如何处理它。这与许多其他类型的通信相同，例如发送电子邮件。
// 如果您需要答案但没有得到答案，您愿意等多久才采取行动？

// 在 Akka 中，答案是根据时间和两种可能的结果来制定的。当你提出要求时，你必须说明你愿意等待多长时间，以及如果你没有得到答案你会怎么做。
// 对于时间规范，您可以使用 Timeout 类。对于您可能会或可能不会收到回复的事实，您检查两个选项，Success 和 Failure。

// 这两个选项构成类 Try[T]，其中 T 是被询问的 actor 的类型。仅当有成功答案时，T 才相关。否则，T 不适用，因为如果 actor 不回答，也没有类型。

// 按照与上一个示例相同的结构，您有一个 App、一个 guardian、一个 Manager 和 Worker，但有一些不同。
// Manager 不再告诉 Worker 该做什么，而是询问他们，这就规定了时间限制。其次，Worker 现在花费大约 3 秒来解析文本。这模拟了一个复杂的解析。
object SimpleQuestionApp extends App {

  val guardian: ActorSystem[Guardian.Command] =
    ActorSystem(Guardian(), "example-ask-without-content")
  guardian ! Guardian.Start(List("text-a", "text-b", "text-c"))

  println("press ENTER to terminate")
  scala.io.StdIn.readLine()
  guardian.terminate()
}
