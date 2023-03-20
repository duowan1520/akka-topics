package com.manning

import akka.actor.typed.ActorSystem

// 在本节中，您学习了如何使用参与者将状态作为值进行管理。
// 您需要在其构造函数中添加状态并将其传递给下一个行为。
// 但是还有另一种方法可以将状态存储在 actor 中。那就是行为。

object WalletStateApp extends App {

  // 在此示例中，钱包存储的总值可以在一定限度内增加或减少。最大值设置为 2，最小值设置为 0（零）以保持简单。
  // 当超过最大/最小值时，不仅钱包停止增加/减少，actor 也停止运行，表明钱包处于不可接受的状态。

  // 钱包的初始值为 0，最大值为 2。这就是您在下一个清单中将 WalletState(0,2) 作为工厂方法的原因。
  // 这意味着 actor 是使用输入参数创建的（更多关于在列表之后创建的内容）。
  val guardian: ActorSystem[WalletState.Command] =
    ActorSystem(WalletState(0, 2), "wallet-state")

  // 一旦创建了钱包，就可以使用其协议发送消息，如清单 2.4 所示。为简单起见，这里只显示钱包的增量。
  guardian ! WalletState.Increase(1)
  guardian ! WalletState.Increase(1)
  guardian ! WalletState.Increase(1)

  println("Press ENTER to terminate")
  scala.io.StdIn.readLine()
  guardian.terminate()

}
