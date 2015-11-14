package bz

import com.netflix.hystrix.{HystrixCommand, HystrixCommandGroupKey}
import com.netflix.hystrix.HystrixCommandKey
import com.netflix.hystrix.HystrixCommand.Setter
import scalaz.\/
import scalaz.syntax.either._
import scalaz.syntax.std.boolean._

/**
 * Typeclass for HxInterface.
 * Must implement hxC which creates an HxCommand
 */
trait HxInterface[M[_]] {

  /**
   * Returns a HxCommand[M[A]] based on given
   * Setter and () => A
   */
  def hxC[A](s: Setter)(fn: () => A): HxCommand[M[A]]

  /**
   * Executes new HxCommand[M[A]] based on given
   * Setter and () => A
   */
  def mHx[A](s: Setter)(fn: () => A): M[A] =
    hxC(s)(fn).execute()
}

/**
 * Throwable types representing various
 * Hystrix specific failure states
 */
object HxThrowables {
  sealed trait HxThrowable
  class HxShortCircuit(m: String) extends Throwable(m) with HxThrowable
  class HxMaxedOut(m: String) extends Throwable(m) with HxThrowable
  class HxTimedOut(m: String) extends Throwable(m) with HxThrowable
  class HxMaxedOutTimedOut(m: String) extends Throwable(m) with HxThrowable
}

/**
 * Base class for instances of HxInterface typeclass
 * extending HystrixCommand.
 *
 * Provides access to correct type of Throwable in
 * case of failure
 */
abstract class HxCommand[A](s: Setter) extends HystrixCommand[A](s) {
  import HxThrowables._

  /**
   * Returns Throwable explaining a failure.
   *
   * Either the Throwable generated by the provided
   * run() fn, or the appropriate HxThrowable when
   * failure is caused by Hystrix intervention
   */
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

/**
 * Implicit instances of HxInterface typeclass
 */
object HxInterfaces {

  /**
   * Option instance of HxInterface.
   * Returns Some(A) on success and None on failure.
   */
  implicit val optionHx = new HxInterface[Option] {

    def hxC[A](s: Setter)(
                        fn: () => A): HxCommand[Option[A]] =
      new HxCommand[Option[A]](s) {
        override def run(): Option[A] = Some(fn())
        override def getFallback(): Option[A] = None
      }
  }

  /**
   * \/ with Throwable on the left
   */
  type THD[A] = \/[Throwable, A]

  /**
   * [[THD]] instance of HxInterface.
   * Returns \/-[A] on success and -\/[Throwable] on failure
   */
  implicit val djHx = new HxInterface[THD] {

      def hxC[A](s: Setter)(
                          fn: () => A): HxCommand[THD[A]] =
        new HxCommand[\/[Throwable, A]](s) {
          override def run(): \/[Throwable, A] =
            fn().right[Throwable]
          override def getFallback(): \/[Throwable, A] =
            new Throwable("overriden-by-mHx").left[A]
        }

      override def mHx[A](s: Setter)(fn: () => A): THD[A] =  {
        val c = hxC(s)(fn)
        c.execute().leftMap(_ => c.getThr)
      }
  }
}

/**
 * Quasi HxInterface that supports context aware user code.
 *
 * Useful when you may want to return a partial success but
 * maintain control of whether Hystrix considers excution
 * success or failure.
 */
object HxControl {
  sealed trait HxResult[A]
  case class HxFail[A](get: Option[A] = None,
                       th: Option[Throwable] = None)
    extends HxResult[A]
  case class HxOk[A](get: A) extends HxResult[A]

  /**
   * Returns an HxCommand[HxResult[A]] which
   * executes the given () => HxResult[A]
   *
   * Return an HxFail from provided fn to
   * cause Hystrix to register a failure, otherwise
   * return an HxOk
   *
   * Failures caused by Hystrix will be returned with
   * an HxFail according to the getThr method
   */
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
            case HxFail(a, _) =>
              HxFail(a, Some(new Throwable("overriden-by-mHx")))
            case k: HxOk[A] => k
          }
        }.getOrElse {
          HxFail(None, Some(new Throwable("overriden-by-mHx")))
        }
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

/**
 * Constructors for HystrixCommand.Setter
 */
object HX {

  /**
   * Returns HystrixCommand.Setter with
   * GroupKey and CommandKey both set to provided String k
   */
  def command(k: String): Setter =
    Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey(k))
          .andCommandKey(HystrixCommandKey.Factory.asKey(k))

  /**
   * Returns HystrixCommand.Setter with
   * GroupKey set to String g and
   * CommandKey set to String c
   */
  def command(g: String, c: String): Setter =
    Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey(g))
          .andCommandKey(HystrixCommandKey.Factory.asKey(c))


  /**
   * Creates a new HxCommand with the given fn and fn as
   * run and getFallback functions, and the given
   * HystrixCommand.Setter s, and executes it
   */
  def instance[A](fn: () => A, fb: () => A)(s: Setter): A =
    new HxCommand[A](s) {
      override def run(): A = fn()
      override def getFallback(): A = fb()
    }.execute()
}
