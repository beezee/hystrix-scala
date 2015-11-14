package bz.syntax

import bz.HX
import bz.HxControl._
import bz.HxInterface
import com.netflix.hystrix.HystrixCommand.Setter
import com.netflix.hystrix.HystrixCommandProperties.{Setter => CSetter}
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
     * Lift a Setter into a HystrixCommand with
     * run method of r and getFallback method of fb
     * and execute
     */
    def run[A](r: () => A, fb: () => A): A =
      HX.instance(r, fb)(s)

    /**
     * Lift a Setter into a HystrixCommand and
     * execute using the given () => A
     */
    def run[M[_]](fn: () => A)(implicit hxi: HxInterface[M]): M[A] =
      hxi.mHx(s)(fn)

    /**
     * Returns original Setter with added command properties,
     * as configured by the provided fn
     */
    def config(fn: CSetter => CSetter): Setter =
      s.andCommandPropertiesDefaults(fn(CSetter()))
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
