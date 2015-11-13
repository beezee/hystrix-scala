package bz

import com.netflix.hystrix.{HystrixCommand, HystrixCommandGroupKey}
import com.netflix.hystrix.HystrixCommandKey
import com.netflix.hystrix.HystrixCommand.Setter
import scalaz.\/
import scalaz.Unapply
import scalaz.syntax.either._
import scalaz.syntax.std.boolean._

trait HxInterface[M[_]] {

  def hxC[A](s: Setter)(fn: () => A): HxCommand[M[A]]

  def mHx[A](s: Setter)(fn: () => A): M[A] =
    hxC(s)(fn).execute()
}

abstract class HxCommand[A](s: Setter) extends HystrixCommand[A](s) {
  class HxShortCircuit(m: String) extends Throwable(m)
  class HxMaxedOut(m: String) extends Throwable(m)
  class HxTimedOut(m: String) extends Throwable(m)
  class HxMaxedOutTimedOut(m: String) extends Throwable(m)

  def getThr: Throwable =
    (isResponseShortCircuited, isResponseRejected,
      isResponseTimedOut) match {
      case (false, false, false) =>
            Option(getFailedExecutionException)
              .getOrElse(new Throwable("unknown-hx-failure"))
      case (true, _, _) => new HxShortCircuit("hx-short-circuited")
      case (false, true, false) => new HxMaxedOut("hx-maxed-out")
      case (false, false, true) => new HxTimedOut("hx-timed-out")
      case (false, true,true) =>
        new HxMaxedOutTimedOut("hx-maxed-out-timed-out")
    }
}

object HxInterfaces {

  implicit val optionHx = new HxInterface[Option] {

    def hxC[A](s: Setter)(
                        fn: () => A): HxCommand[Option[A]] =
      new HxCommand[Option[A]](s) {
        override def run(): Option[A] = Some(fn())
        override def getFallback(): Option[A] = None
      }
  }

  type THD[A] = \/[Throwable, A]
  implicit val djHx = new HxInterface[THD] {

      def hxC[A](s: Setter)(
                          fn: () => A): HxCommand[THD[A]] =
        new HxCommand[\/[Throwable, A]](s) {
          override def run(): \/[Throwable, A] =
            fn().right[Throwable]
          override def getFallback(): \/[Throwable, A] =
            getThr.left[A]
        }

      override def mHx[A](s: Setter)(fn: () => A): THD[A] =  {
        val c = hxC(s)(fn)
        c.execute().leftMap(_ => c.getThr)
      }
  }
}

object HxControl {
  sealed trait HxResult[A]
  case class HxFail[A](get: Option[A] = None,
                       th: Option[Throwable] = None)
    extends HxResult[A]
  case class HxOk[A](get: A) extends HxResult[A]

  def controlCommand[A](s: Setter)(
                   fn: () => HxResult[A]): HxCommand[HxResult[A]] =
    new HxCommand[HxResult[A]](s) {
      @volatile private var res: HxResult[A] = null

      override def run(): HxResult[A] = {
        res = fn()
        res match {
          case f@HxFail(a, th) => {
            th.orElse {
              Some(new Exception(a.toString))
            }.foreach(x => throw x)
            f
          }
          case _: HxOk[A] => res
        }
      }

      override def getFallback(): HxResult[A] =
        Option(res).map { r =>
          r match {
            case HxFail(a, _) => HxFail(a, Some(getThr))
            case k: HxOk[A] => k
          }
        }.getOrElse(HxFail(None, Some(getThr)))
    }

    def controlInterface[A](s: Setter)(fn: () => HxResult[A]):
    HxResult[A] = {
      val c = controlCommand(s)(fn)
      val r = c.execute()
      r match {
        case o@HxFail(a, t) =>
          t.map(_ => HxFail(a, Some(c.getThr))).getOrElse(o)
        case _ => r
      }
    }

}

object HX {

  def command(k: String): Setter =
    Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey(k))
          .andCommandKey(HystrixCommandKey.Factory.asKey(k))

  def command(g: String, c: String): Setter =
    Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey(g))
          .andCommandKey(HystrixCommandKey.Factory.asKey(c))
}
