package bz

import com.netflix.hystrix.HystrixCommand
import com.netflix.hystrix.HystrixCommandGroupKey
import scalaz.\/
import scalaz.Unapply
import scalaz.syntax.either._
import scalaz.syntax.std.boolean._

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
      class HxShortCircuit extends Throwable

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
