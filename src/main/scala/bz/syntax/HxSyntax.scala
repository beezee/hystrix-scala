package bz.syntax

import bz.HxInterface
import com.netflix.hystrix.HystrixCommandGroupKey

object hx {

  implicit class HxLift[A](fn: () => A) {

    def liftHx(gk: HystrixCommandGroupKey): Option[A] =
      HxInterface.mHx(gk)(fn)
  }

  implicit class GKLift(gk: HystrixCommandGroupKey) {

    def toOption[A](fn: () => A): Option[A] =
      HxInterface.mHx(gk)(fn)
  }
}
