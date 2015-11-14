package bz

import HxInterfaces._
import syntax.hx._
import org.scalacheck.{Gen, Properties}
import org.scalacheck.Arbitrary.{arbitrary => arb}
import org.scalacheck.Prop.forAll
import scalaz.{\/, -\/}
import scalaz.syntax.either._

object HXTest extends Properties("HX") {

  val thI = for {
    i <- Gen.choose(0, 100000)
    tm <- Gen.alphaStr.suchThat(_ != "")
    th = new Exception(tm)
  } yield (th, i)

  property("Converts circuit breaker tripped into meaningful message") =
    forAll(thI) { (thI) =>
      val (th, i) = thI
      val t = HX.command("test")
                .config(_.withCircuitBreakerRequestVolumeThreshold(2)
                         .withCircuitBreakerErrorThresholdPercentage(10)
                         .withCircuitBreakerForceOpen(true))
      t.run[THD]{ () => throw th; i }
       .swap.map(_.getMessage).getOrElse("") == "hx-short-circuited"
    }
}
