package suggestions

import language.postfixOps
import scala.concurrent._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Try, Success, Failure}
import rx.lang.scala._
import org.scalatest._
import gui._

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import scala.collection.mutable.ListBuffer
import rx.lang.scala.subscriptions.Subscription

@RunWith(classOf[JUnitRunner])
class WikipediaApiTest extends FunSpec {

  object mockApi extends WikipediaApi {
    def wikipediaSuggestion(term: String) = Future {
      if (term.head.isLetter) {
        for (suffix <- List(" (Computer Scientist)", " (Footballer)")) yield term + suffix
      } else {
        List(term)
      }
    }

    def wikipediaPage(term: String) = Future {
      "Title: " + term
    }
  }

  import mockApi._

  describe("WikipediaApi") {

    it("should make the stream valid using sanitized") {
      val notvalid = Observable("erik", "erik meijer", "martin")
      val valid: Observable[String] = notvalid.sanitized

      var count = 0
      var completed = false

      val sub = valid.subscribe(
        term => {
          assert(term.forall(_ != ' '))
          count += 1
        },
        t => assert(false, s"stream error $t"),
        () => completed = true
      )
      assert(completed && count == 3, "completed: " + completed + ", event count: " + count)
    }

    it("should make the stream with multiple spaces valid using sanitized") {
      val notvalid = Observable("   erik", "erik    meijer", "  martin   ")
      val valid: Observable[String] = notvalid.sanitized

      var count = 0
      var completed = false

      val sub = valid.subscribe(
        term => {
          assert(term.forall(_ != ' '))
          count += 1
        },
        t => assert(false, s"stream error $t"),
        () => completed = true
      )
      assert(completed && count == 3, "completed: " + completed + ", event count: " + count)
    }

    it("should divide observable into Success/Failure using recovered") {
      val obsl1: Observable[Int] = Observable(1, 2, 3) ++ Observable(new Exception("odd guy"))
      val obsl2: Observable[Try[Any]] = obsl1.recovered

      var success, failure = 0

      obsl2 subscribe { res =>
        res match {
          case Success(x) => success = success + 1
          case Failure(ex) => failure = failure + 1
        }
      }

      assert(success === 3)
      assert(failure === 1)
    }

    it("should stop emitting values after timeout") {
      val clock = Observable.interval(0.3 second)
      val timedOut = clock.timedOut(2)
      assert(timedOut.toBlockingObservable.toList.length === 6)
    }

    it("should correctly use concatRecovered") {
      val requests: Observable[Int] = Observable(1, 2, 3)
      val remoteComputation: (Int) => Observable[Int] = (n: Int) => Observable(0 to n)
      val responses: Observable[Try[Int]] = requests concatRecovered remoteComputation
      val sum: Observable[Int] = responses.foldLeft(0) {
        (acc, tn) =>
          tn match {
            case Success(n) => acc + n
            case Failure(t) => throw t
          }
      }
      var total = -1
      val sub = sum.subscribe {
        s => {
          print(s +" ")
          total = s
        }
      }
      assert(total == (1 + 1 + 2 + 1 + 2 + 3), s"Sum: $total")
    }

    it("should emit values post exception using concatRecovered") {
      val exception = new Exception("test")
      val expected = List(Success(2), Success(1), Failure(exception), Success(1), Success(0), Failure(exception))

      val actual = Observable(2, 1).concatRecovered( i => Observable(i, i-1) ++ Observable(exception)).toBlockingObservable.toList
      assert(actual === expected)
    }
  }
}