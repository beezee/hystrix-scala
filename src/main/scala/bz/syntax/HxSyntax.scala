package bz.syntax

import bz.HxControl._
import bz.HxInterface
import com.netflix.hystrix.HystrixCommand.Setter
import scalaz.\/

object hx {

  implicit class HxLift[M[_], A](fn: () => A)(
                                implicit hxi: HxInterface[M]) {
    def liftHx(s: Setter): M[A] =
      hxi.mHx(s)(fn)
  }

  implicit class GKLift[A](s: Setter){
    def run[M[_]](fn: () => A)(implicit hxi: HxInterface[M]): M[A] =
      hxi.mHx(s)(fn)
  }

  implicit class DjLift(s: Setter){
    def runC[A](fn: () => HxResult[A]): HxResult[A] =
      controlInterface(s)(fn)
  }
}
