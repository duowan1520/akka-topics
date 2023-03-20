package questionwithpayload

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.util.Timeout

import scala.concurrent.duration.SECONDS
import scala.util.Failure
import scala.util.Success

// 当您为请求指定 createRequest 参数时，您不能像以前那样只依赖 Worker.Parse.apply() 。
// 请记住，createRequest 需要一个只有一个参数的函数，以便系统可以为您传递 replyTo。
// 但是如果你的 Worker.Parse.apply 有两个参数，text 和 replyTo，你不能只把它传递给 createRequest。
// 您需要创建一个函数来填充文本参数并保留 replyTo 参数未填充。您可以使用一种称为柯里化的技术来做到这一点。

// Currying 允许您将一些参数传递给函数并将其他参数留空。柯里化的结果是一个参数比原始函数少的函数。
// 例如，如果你有一个像 multiplication(x: Int)(y: Int) = x * y 这样的函数，你可以像 multiplication(4) 一样柯里化它，也就是说，你只传递 x，而不传递 y。
// 通过这种方式，您将获得一个新函数 multiplicationCurried(z: Int) = 4 * z，您现在可以在您的程序中使用该函数，例如 multiplicationCurried(3)，结果为 12。

// 在 Scala 中，您必须显式指定何时可以柯里化函数。为此，您可以一次一个地用括号分隔要传递的输入变量。对于乘法示例，您可以这样做：
// def multiplication(x: Int)(y: Int)
// 所以你可以将它部分地用作 multiplication(4)_ 。请注意，下划线是表示 y 未通过的必要条件。这会返回 multiplicationCurried，然后您可以在程序的其他地方使用它。
object Manager {

  sealed trait Command

  final case class Delegate(texts: List[String]) extends Command

  final case class Report(outline: String) extends Command

  def apply(): Behavior[Command] =
    Behaviors.setup { context =>
      implicit val timeout: Timeout = Timeout(1, SECONDS)

      // 在以下代码片段中，您有一个柯里化函数，它为您提供解析工作者所需的签名。
      // 例如，如果您将“text-a”之类的文本传递给此函数，您将取回所需的函数。这与 createRequest 的签名相匹配：ActorRef[Res] => Req。
      def auxCreateRequest(text: String)(
          replyTo: ActorRef[Worker.Response]): Worker.Parse =
        Worker.Parse(text, replyTo)

      Behaviors.receiveMessage { message =>
        message match {
          case Delegate(texts) =>
            texts.map { text =>
              val worker: ActorRef[Worker.Command] =
                context.spawn(Worker(), s"worker-$text")
              // 这种从直接传递 Worker.Parse 到创建 auxCreateRequest 并将该柯里化函数传递给 ask 的变化是启用有效负载传递所需的。
              // 有了这个，您在本节中了解到在询问演员时必须包括有效载荷。
              context.ask(worker, auxCreateRequest(text)) {
                case Success(_) =>
                  Report(s"$text read by ${worker.path.name}")
                case Failure(ex) =>
                  Report(
                    s"reading '$text' has failed with [${ex.getMessage()}")
              }
            }
            Behaviors.same
          case Report(outline) =>
            context.log.info(outline)
            Behaviors.same
        }
      }
    }
}
