package com.manning

import akka.actor.typed.ActorSystem

// 应用程序是可执行程序。您可以通过定义一个从 scala.App 扩展的对象来创建此应用程序。像这样。
// 这是创建可运行类的简单方法。在示例结束时，您将学习如何运行此类。
object WalletApp extends App {

  // 在 Scala 中，泛型类型——等同于 Java 泛型——由括号中的字母标识。
  // def apply[T](guardianBehavior: Behavior[T], name: String): ActorSystem[T]
  // Scala 中的每个对象（如 ActorSystem）都是通过 apply 方法创建的，但也可以单独通过对象名称创建。
  // 例如，构造函数 ActorSystem(myParameters) 引用了 ActorSystem.apply(myParameters) 。
  // 另一个例子是 Wallet()，它引用了 Wallet.apply()。
  // 考虑到这一点，您现在可以看到在 ActorSystem(Wallet(), "wallet") 中第一个参数如何引用钱包构造函数，即它的 apply 方法。
  val guardian: ActorSystem[Int] = ActorSystem(Wallet(), "wallet")

  // 有两点值得注意。
  // 首先，钱包 actor 被实例化为一个 ActorSystem，其类型为 [Int]。
  //      这表明你已经知道，这个 actor 只能接收整数作为消息。
  // 其次，您可能还记得第 1 章中提到的 ActorSystem 是对 actor 的引用，即 ActorRef。

  // 这是您可以用来向参与者发送消息的地址。为此，您可以使用 tell 方法，该方法通常用 bang 运算符表示，即 !。
  // 以下代码段表示将数字 1 发送到 guardian 地址。
  // 结合所有这些成分，这就是它的样子——先前定义的 actor 及其实例化并向它发送两条消息。
  guardian ! 1
  guardian ! 10
  // 您首先向 actor 发送整数 1，然后是整数 10。actor 的类型是 Int，正如在 ActorSystem[Int] 中明确设置的那样。
  // 但您也可以验证情况是否如此，因为代码可以编译。特别是以下几行

  // 如果将 Int 发送给无法接收 Int 的 actor，编译时会出错。代码无法编译，更不用说运行应用程序了。
  // 这种类型检查，即如果您尝试发送消息类型以外的内容，应用程序将不会编译的事实，是对 Akka 以前版本的最大改进之一。
  // 另一个优点是，如果您在重构时更改了 actor 的协议，并且旧协议仍在代码中的其他地方使用，则编译器会立即进行救援。
  // 这个新功能的重要性怎么估计都不过分。

  // 正在运行的代码在 .readLine() 处停止并等待。当您按下 Enter 时，代码将继续并终止系统。
  println("Press ENTER to terminate")
  scala.io.StdIn.readLine()

  // 您应该优雅地终止 ActorSystem，以便系统可以优雅地关闭所有正在运行的模块，例如 HTTP 或 DB 连接。
  // 为此，您需要使用 .terminate() 终止监护人——即 ActorSystem。
  guardian.terminate()

}
