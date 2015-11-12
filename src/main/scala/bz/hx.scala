package bz

import com.netflix.hystrix.HystrixCommand
import com.netflix.hystrix.HystrixCommandGroupKey

trait HxInterface[M[_]] {

  protected def hxC[A](k: HystrixCommandGroupKey)(
                      fn: () => A): HystrixCommand[M[A]]

  def mHx[A](k: HystrixCommandGroupKey)(
                      fn: () => A): M[A] =
    hxC(k)(fn).execute()
}

object HxInterfaces {

  implicit val optionHx = new HxInterface[Option] {

    protected def hxC[A](k: HystrixCommandGroupKey)(
                        fn: () => A): HystrixCommand[Option[A]] =
      new HystrixCommand[Option[A]](k) {
        override def run(): Option[A] = Some(fn())
        override def getFallback(): Option[A] = None
      }
  }
}
