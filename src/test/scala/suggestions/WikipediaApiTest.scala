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

    ignore("should correctly use concatRecovered") {
      val requests = Observable(1, 2, 3)
      val remoteComputation = (n: Int) => Observable(0 to n)
      val responses = requests concatRecovered remoteComputation
      val sum = responses.foldLeft(0) {
        (acc, tn) =>
          tn match {
            case Success(n) => acc + n
            case Failure(t) => throw t
          }
      }
      var total = -1
      val sub = sum.subscribe {
        s => total = s
      }
      assert(total == (1 + 1 + 2 + 1 + 2 + 3), s"Sum: $total")
    }
  }
}