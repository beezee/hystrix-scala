package bz.syntax

import bz.HxInterface
import com.netflix.hystrix.HystrixCommandGroupKey

object hx {

  implicit class HxLift[M[_], A](fn: () => A)(
                                implicit hxi: HxInterface[M]) {

    def liftHx(gk: HystrixCommandGroupKey): M[A] =
      hxi.mHx(gk)(fn)
  }

  implicit class GKLift[A](gk: HystrixCommandGroupKey){
    def run[M[_]](fn: () => A)(implicit hxi: HxInterface[M]): M[A] =
      hxi.mHx(gk)(fn)
  }
}
