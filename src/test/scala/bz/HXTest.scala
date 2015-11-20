package bz

import HxInterfaces._
import syntax.hx._
import org.scalacheck.{Gen, Properties}
import org.scalacheck.Arbitrary.{arbitrary => arb}
import org.scalacheck.Prop.forAll
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scalaz.{\/, -\/}
import scalaz.syntax.either._

object HXTest extends Properties("HX") {

  val c = Gen.alphaStr.suchThat(_ != "")

  val thI = for {
    i <- Gen.choose(0, 100000)
    tm <- Gen.alphaStr.suchThat(_ != "")
    th = new Exception(tm)
  } yield (th, i)

  property("Converts circuit breaker tripped into meaningful message") =
    forAll(thI, c, c) { (thI, g, c) =>
      val (th, i) = thI
      val t = HX.command(g, c)
                .config(_.withCircuitBreakerForceOpen(true))
      t.run[THD]{ () => throw th; i }
       .swap.map(_.getMessage).getOrElse("") == "hx-short-circuited"
    }

  property("Converts timed out into meaningful message") =
    forAll(Gen.choose(1200, 20000), c, c) {
      (ms, g, c) =>
        val t = HX.command(g, c)
                  .config(_.withExecutionTimeoutInMilliseconds(1))
        t.run[THD]{ () => Thread.sleep(ms.toLong) }
          .swap.map(_.getMessage).getOrElse("") == "hx-timed-out"
      }

  property("Converts maxed out into meaningful message") =
    forAll(c, c) { (g, c) =>
      val t = HX.command(g, c)
                .config(_.usingSemaphore
                 .withExecutionIsolationSemaphoreMaxConcurrentRequests(0))
      t.run[THD]{ () => () }
       .swap.map(_.getMessage).getOrElse("") == "hx-maxed-out"
    }
}
