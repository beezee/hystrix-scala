##hystrix-toOption

I'm sure it's missing plenty of stuff, I only looked at the hello world.

Just had an itch.

If you wanna use it, you can do this:

```scala
import com.netflix.hystrix.HystrixCommandGroupKey
import bz.syntax.hx._
import bz.HxInterfaces._

val t = HystrixCommandGroupKey.Factory.asKey("ExampleGroup")
//t: com.netflix.hystrix.HystrixCommandGroupKey = com.netflix.hystrix.HystrixCommandGroupKey$Factory$HystrixCommandGroupDefault@7b19f87b

val x = t.run { () => 1 }.map(_ + 2)
//x: Option[Int] = Some(3)

val y = t.toOption { () => throw new Exception("foobard"); 3 }.map(_ + 2)
res3: Option[Int] = None

// or the other way around, in case you're weird

val iamweird = { () => 1 }.liftHx(t)
//iamweird: Option[Int] = Some(1)
```

Shoutout to @rboccuzzi for working through these ideas with me
