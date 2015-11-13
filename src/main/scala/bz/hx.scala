package bz

import HxShortCircuit._
import com.netflix.hystrix.HystrixCommand
import com.netflix.hystrix.HystrixCommandGroupKey
import scalaz.\/
import scalaz.Unapply
import scalaz.syntax.either._
import scalaz.syntax.std.boolean._

object HxShortCircuit {
  class HxShortCircuit extends Throwable
}

trait HxInterface[M[_]] {

  def hxC[A](k: HystrixCommandGroupKey)(
                      fn: () => A): HystrixCommand[M[A]]

  def mHx[A](k: HystrixCommandGroupKey)(
                      fn: () => A): M[A] =
    hxC(k)(fn).execute()
}

object HxInterfaces {

  implicit val optionHx = new HxInterface[Option] {

    def hxC[A](k: HystrixCommandGroupKey)(
                        fn: () => A): HystrixCommand[Option[A]] =
      new HystrixCommand[Option[A]](k) {
        override def run(): Option[A] = Some(fn())
        override def getFallback(): Option[A] = None
      }
  }

  type THD[A] = \/[Throwable, A]
  implicit val djHx = new HxInterface[THD] {

      def hxC[A](k: HystrixCommandGroupKey)(
                          fn: () => A): HystrixCommand[THD[A]] =
        new HystrixCommand[\/[Throwable, A]](k) {
          override def run(): \/[Throwable, A] =
            fn().right[Throwable]
          override def getFallback(): \/[Throwable, A] =
            isCircuitBreakerOpen.fold(
              (new HxShortCircuit),
            Option(getFailedExecutionException)
              .getOrElse(new Throwable("unknown-hx-failure")))
            .left[A]
        }
  }
}

object HxControl {
  sealed trait HxResult[A]
  case class HxFail[A](get: Option[A] = None,
                       th: Option[Throwable] = None)
    extends HxResult[A]
  case class HxOk[A](get: A) extends HxResult[A]

  def controlInterface[A](k: HystrixCommandGroupKey)(
                   fn: () => HxResult[A]): HxResult[A] =
    new HystrixCommand[HxResult[A]](k) {
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
            case HxFail(a, _) => {
              val t = isCircuitBreakerOpen.fold(
                        (new HxShortCircuit),
                      Option(getFailedExecutionException)
                        .getOrElse(new Throwable("unknown-hx-failure")))
              HxFail(a, Some(t))
            }
            case k: HxOk[A] => k
          }
        }.getOrElse(HxFail(th = Some(new HxShortCircuit)))
    }.execute()
}
