package com.manning

import akka.actor.typed.ActorSystem

object WalletOnOffApp extends App {

  val guardian: ActorSystem[WalletOnOff.Command] =
    ActorSystem(WalletOnOff(), "wallet-on-off")

  // 请注意，默认状态是激活的。以下代码段显示了如何增加、停用和激活钱包。
  guardian ! WalletOnOff.Increase(1)
  guardian ! WalletOnOff.Deactivate
  guardian ! WalletOnOff.Increase(1)
  guardian ! WalletOnOff.Activate
  guardian ! WalletOnOff.Increase(1)

  println("Press ENTER to terminate")
  scala.io.StdIn.readLine()
  guardian.terminate()

}
