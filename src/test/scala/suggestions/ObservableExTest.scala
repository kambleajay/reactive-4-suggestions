package suggestions

import scala.collection._
import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Try, Success, Failure}
import scala.swing.event.Event
import scala.swing.Reactions.Reaction
import rx.lang.scala._
import org.scalatest._
import gui._

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import suggestions.observablex.ObservableEx

@RunWith(classOf[JUnitRunner])
class ObservableExTest extends FunSpec {

  describe("ObservableEx") {

    it("should emit future values to observable") {
      val obsl = ObservableEx {
        val p = Promise[Int]
        p.success(1)
        p.future
      }
      val observed = mutable.Buffer[Int]()
      val sub = obsl subscribe {
        observed += _
      }
      assert(observed.toSeq === Seq(1))
    }

    it("should not emit any value in case of error") {
      val obsl = ObservableEx {
        val p = Promise[Int]
        p.failure(new Exception("error"))
        p.future
      }
      val observed = mutable.Buffer[Int]()
      val sub = obsl subscribe(
        x => observed += x,
        e => println("exception"),
        () => println("complete")
        )
      assert(observed === Nil)
    }

  }

}