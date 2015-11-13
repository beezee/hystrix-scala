##hystrix-scala

Provides a small functional interface to Hystrix.

```scala
import bz.HX
import bz.syntax.hx._
import bz.HxInterfaces._

val s = HX.command("Example")
//s: com.netflix.hystrix.HystrixCommand.Setter = com.netflix.hystrix.HystrixCommand$Setter@7f414b69

val x = s.run[THD] { () => 1 }
//x: bz.HxInterfaces.THD[Int] = \/-(1)

val y = s.run[THD] { () => throw new Exception("fail"); 1 }
//y: bz.HxInterfaces.THD[Int] = -\/(java.lang.Exception: fail)

val a = s.run[Option] { () = 1 }
//a: Option[Int] = Some(1)

val b = s.run[Option] { () => throw new Exception("fail"); 1 }
//b: Option[Int] = None
```

See src/main/scala/bz/hx.scala for Throwable types that represent
Hystrix specific conditions (at capacity, timeout, circuit open), as
well as a variant on the above that lets you control whether to count
operation as a failure via Hystrix within your provided function, while
retaining partial success.

Shoutout to @rboccuzzi for working through these ideas with me
