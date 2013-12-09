package suggestions
package gui

import scala.collection.mutable.ListBuffer
import scala.collection.JavaConverters._
import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.swing._
import scala.util.{Try, Success, Failure}
import scala.swing.event._
import swing.Swing._
import javax.swing.UIManager
import Orientation._
import rx.subscriptions.CompositeSubscription
import rx.lang.scala.Observable
import rx.lang.scala.Subscription
import observablex._
import search._
import scala.concurrent.duration._

object WikipediaSuggest extends SimpleSwingApplication with ConcreteSwingApi with ConcreteWikipediaApi {

  {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    } catch {
      case t: Throwable =>
    }
  }

  def top = new MainFrame {

    /* gui setup */

    title = "Query Wikipedia"
    minimumSize = new Dimension(900, 600)

    val button = new Button("Get") {
      icon = new javax.swing.ImageIcon(javax.imageio.ImageIO.read(this.getClass.getResourceAsStream("/suggestions/wiki-icon.png")))
    }
    val searchTermField = new TextField
    val suggestionList = new ListView(ListBuffer[String]())
    val status = new Label(" ")
    val editorpane = new EditorPane {

      import javax.swing.border._

      border = new EtchedBorder(EtchedBorder.LOWERED)
      editable = false
      peer.setContentType("text/html")
    }

    contents = new BoxPanel(orientation = Vertical) {
      border = EmptyBorder(top = 5, left = 5, bottom = 5, right = 5)
      contents += new BoxPanel(orientation = Horizontal) {
        contents += new BoxPanel(orientation = Vertical) {
          maximumSize = new Dimension(240, 900)
          border = EmptyBorder(top = 10, left = 10, bottom = 10, right = 10)
          contents += new BoxPanel(orientation = Horizontal) {
            maximumSize = new Dimension(640, 30)
            border = EmptyBorder(top = 5, left = 0, bottom = 5, right = 0)
            contents += searchTermField
          }
          contents += new ScrollPane(suggestionList)
          contents += new BorderPanel {
            maximumSize = new Dimension(640, 30)
            add(button, BorderPanel.Position.Center)
          }
        }
        contents += new ScrollPane(editorpane)
      }
      contents += status
    }

    val eventScheduler = SchedulerEx.SwingEventThreadScheduler

    /**
     * Observables
     * You may find the following methods useful when manipulating GUI elements:
     * `myListView.listData = aList` : sets the content of `myListView` to `aList`
     * `myTextField.text = "react"` : sets the content of `myTextField` to "react"
     * `myListView.selection.items` returns a list of selected items from `myListView`
     * `myEditorPane.text = "act"` : sets the content of `myEditorPane` to "act"
     */

    // TO IMPLEMENT
    val searchTerms: Observable[String] = searchTermField.textValues.sanitized

    // TO IMPLEMENT
    val suggestions: Observable[Try[List[String]]] = {
      searchTerms.concatRecovered {
        term =>
          val r1: Future[List[String]] = wikipediaSuggestion(term)
          val r2: Observable[List[String]] = ObservableEx(r1)
          r2
      }
    }

    // TO IMPLEMENT
    val suggestionSubscription: Subscription = suggestions.observeOn(eventScheduler).subscribe(
      (x: Try[List[String]]) => x match {
        case Success(xs) => suggestionList.listData = xs
        case Failure(err) => status.text = err.getMessage
      },
      (ex: Throwable) => status.text = ex.getMessage
    )

    // TO IMPLEMENT
    //button clicks -> suggestion list selected text
    val selections: Observable[String] = button.clicks.map {
      btn =>
        val noOfSelections = suggestionList.selection.items.size
        noOfSelections match {
          case 0 => None
          case x => Some(suggestionList.selection.items.head)
        }
    }.filter(opt => opt != None).map(_.get)

    // TO IMPLEMENT
    val pages: Observable[Try[String]] = selections map {
      selection => {
        Try { Await.result(wikipediaPage(selection), 5 seconds) }
      }
    }

    // TO IMPLEMENT
    val pageSubscription: Subscription = pages.observeOn(eventScheduler) subscribe {
      x => x.map(s => editorpane.text = s)
    }

  }

}


trait ConcreteWikipediaApi extends WikipediaApi {
  def wikipediaSuggestion(term: String): Future[List[String]] = Search.wikipediaSuggestion(term)
  def wikipediaPage(term: String): Future[String] = Search.wikipediaPage(term)
}


trait ConcreteSwingApi extends SwingApi {
  type ValueChanged = scala.swing.event.ValueChanged

  object ValueChanged {
    def unapply(x: Event) = x match {
      case vc: ValueChanged => Some(vc.source.asInstanceOf[TextField])
      case _ => None
    }
  }

  type ButtonClicked = scala.swing.event.ButtonClicked

  object ButtonClicked {
    def unapply(x: Event) = x match {
      case bc: ButtonClicked => Some(bc.source.asInstanceOf[Button])
      case _ => None
    }
  }

  type TextField = scala.swing.TextField
  type Button = scala.swing.Button
}
