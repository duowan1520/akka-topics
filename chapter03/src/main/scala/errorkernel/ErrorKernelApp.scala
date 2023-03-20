package errorkernel

import akka.actor.typed.ActorSystem

// 此应用程序中的 actors 是创建 Manager 的 Guardian，Manager 又创建多个 worker。
// Guardian 将要解析的文本列表传递给 Manager，Manager 又将其传递给 worker 进行解析。
object ErrorKernelApp extends App {

  // 开始时，应用程序将工作(这些文本字符串列表)传递给 guardian, 它沿着链向下发送。
  // 当 workers 完成后，他们会通知 Manager。但是，要理解此通知，Manager 需要一个适配器。
  val guardian: ActorSystem[Guardian.Command] =
    ActorSystem(Guardian(), "error-kernel")
  guardian ! Guardian.Start(List("-one-", "--two--"))

  println("press ENTER to terminate")
  scala.io.StdIn.readLine()
  guardian.terminate()
}
