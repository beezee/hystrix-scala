package bz.syntax

import bz.HxControl._
import bz.HxInterface
import com.netflix.hystrix.HystrixCommand.Setter
import scalaz.\/

/**
 * Implicit classes decorating functions and Setters
 * with methods for execution inside of a HystrixCommand
 */
object hx {

  /**
   * Implicit class decorating functions of arity 0
   */
  implicit class HxLift[A](fn: () => A) {

    /**
     * Lift a function of arity 0 into a
     * HystrixCommand and execute using
     * the given Setter
     */
    def liftHx[M[_]](s: Setter)(implicit hxi: HxInterface[M]): M[A] =
      hxi.mHx(s)(fn)
  }

  /**
   * Implicit class decorating a HystrixCommand.Setter
   */
  implicit class GKLift[A](s: Setter){

    /**
     * Lift a Setter into a HystrixCommand and
     * execute using the given () => A
     */
    def run[M[_]](fn: () => A)(implicit hxi: HxInterface[M]): M[A] =
      hxi.mHx(s)(fn)
  }

  /**
   * Implicit class decorating a HystrixCommand.Setter
   */
  implicit class ControlLift(s: Setter){

    /**
     * Lift a Setter into a HystrixCommand and
     * execute using the given () => HxResult[A]
     */
    def runC[A](fn: () => HxResult[A]): HxResult[A] =
      controlInterface(s)(fn)
  }
}
