package bz

import com.netflix.hystrix.HystrixCommand
import com.netflix.hystrix.HystrixCommandGroupKey

object HxInterface {

  def mHx[A](k: HystrixCommandGroupKey)(
                      fn: () => A): Option[A] =
    new HystrixCommand[Option[A]](k) {
      override def run(): Option[A] = Some(fn())
      override def getFallback(): Option[A] = None
    }.execute()
}
