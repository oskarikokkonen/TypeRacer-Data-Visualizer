import javafx.scene.text
import javafx.scene.text.{FontPosture, FontWeight}
import scalafx.application.JFXApp3
import scalafx.geometry
import scalafx.geometry.Insets
import scalafx.scene.Scene
import scalafx.scene.canvas.{Canvas, GraphicsContext}
import scalafx.scene.input.{KeyEvent, MouseEvent, ScrollEvent}
import scalafx.scene.layout.{GridPane, StackPane}
import scalafx.scene.paint.Color
import scalafx.scene.paint.Color.*
import scalafx.scene.text.{Font, Text, TextAlignment}
import scalafx.Includes.*

import java.util.{Calendar, GregorianCalendar}
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source
import scala.math.sqrt
import scalafx.scene.effect.DropShadow
import scalafx.scene.shape.Rectangle
import scalafx.scene.text.TextAlignment.Center

import scala.io.StdIn.readLine



object chartUI extends JFXApp3:

////////////////////  variables for chart rendering  ////////////////////

  private var timespan: Long                      = 0L
  private var dataRows: Vector[DataRow]           = Vector()
  private var lastRaceTime: GregorianCalendar     = GregorianCalendar()
  private var firstRaceTime: GregorianCalendar    = GregorianCalendar()
  private var firstRaceTimeInMillis: Long         = 0L
  private var lastRaceNum: Int                    = 0
  private var maxWPM: Int                         = 0
  private var minWPM: Int                         = 0
  private var nearest: Option[DataRow]            = None
  private var loadingFinished: Boolean            = false
  private var loadingText: String                 = "Loading data..."
  private var lastX: Double                       = 0.0
  private var lastY: Double                       = 0.0
  private var draggable: Boolean                  = false
  private var valTextY: Double                    = 10
  private val updateData: Boolean                 = true
  private var userName: String                    = ""

////////////////////  constant values for chart rendering  ////////////////////

  if userName == "" then
    userName = readLine("Enter Your Typeracer UserName: ")

  private val shadow = new DropShadow():
    radius = 15
    color = Color(0, 0, 0, 0.4)

  private val cornerText = new Text():
    fill = White
    text = "No Datapoint Selected"
    margin = Insets(30)
    translateX = 8
    alignmentInParent = scalafx.geometry.Pos.CenterLeft
    font = Font.font("Bauhaus 93", FontWeight.THIN, FontPosture.REGULAR, 15)
    lineSpacing = 10
    this.setEffect(shadow)


  private val cornerRect = new Rectangle():
    width = rectX
    height = rectY
    arcWidth = 20
    arcHeight = 20
    margin = Insets(20)
    fill = Color(0.2, 0.2, 0.2, 1)
    this.setEffect(new DropShadow:
      radius = 5
      color = Color(0, 0, 0, 0.4)
    )

  private val chart1 = new Canvas():
    autosize()
    width = UIx
    height = UIy

  private val loadingScreen = new StackPane():
    children =
      new Canvas():
        autosize()
        width = UIx
        height = UIy
        private val lg = graphicsContext2D
        lg.fill = Color(0.13, 0.14, 0.15, 1)
        lg.fillRect(0, 0, UIx, UIy)
        lg.fill = White
        lg.textAlign = TextAlignment.Center
        lg.fillText("Loading data...", UIx / 2, UIy / 2)

  private val rootGrid = new GridPane():
    add(chart1, 0, 0, 2, 2)
    add(cornerRect, 2, 0, 1, 1)
    add(cornerText, 2, 0, 1, 1)

  ////////////////////  start function  ////////////////////

  def start(): Unit =

    stage = new JFXApp3.PrimaryStage:
      title = "Typeracer charts"
      scene = new Scene(rootGrid):
        fill = Color(0.13, 0.14, 0.15, 1)
      show()

    stage.requestFocus()
    stage.toFront()

    Future({
      while !loadingFinished do
        println(loadingText)
        if loadingText.takeRight(3) == "..." then
          loadingText = "Loading data"
        else if loadingText.takeRight(2) == ".." then
          loadingText = "Loading data..."
        else if loadingText.takeRight(1) == "." then
          loadingText = "Loading data.."
        else
          loadingText = "Loading data."
        renderChart(chart1.graphicsContext2D)
        Thread.sleep(500)
    })

    Future({
      fetchData(updateData)
      loadingFinished = true
      renderChart(chart1.graphicsContext2D)
    })


////////////////////  input methods ////////////////////

  chart1.onMouseDragged = (event: MouseEvent) => {
    if draggable then
      xMove += (event.x - lastX)
      yMove += (event.y - lastY)
      lastX = event.x
      lastY = event.y
      renderChart(chart1.graphicsContext2D)
  }

  chart1.onMousePressed = (event: MouseEvent) => {
    draggable = if event.x < rectX || event.y > rectY then true else false
    lastX = event.x
    lastY = event.y
    chart1.requestFocus()
    renderChart(chart1.graphicsContext2D)
  }

  chart1.onScroll = (event: ScrollEvent) => {
    if !event.controlDown then
      val movX = xMove / xZoom
      val movY = yMove / yZoom
      val zoX = xZoom
      val zoY = yZoom
      xZoom = math.max(xZoom + event.deltaY / (400.0 / xZoom), 0.1)
      yZoom = math.max(yZoom + event.deltaY / (400.0 / yZoom), 0.1)
      xMove = movX * xZoom - (event.x - UIx / 2.0) * (xZoom - zoX) / (xZoom - event.deltaY / (400.0 / xZoom))
      yMove = movY * yZoom - (event.y - UIy / 2.0) * (yZoom - zoY) / (yZoom - event.deltaY / (400.0 / yZoom))
    else
      val movX = xMove / xZoom
      val zoX = xZoom
      xZoom = math.max(xZoom + event.deltaY / (400.0 / xZoom), 0.1)
      xMove = movX * xZoom - (event.x - UIx / 2.0) * (xZoom - zoX) / (xZoom - event.deltaY / (400.0 / xZoom))
    xMove += event.deltaX
    renderChart(chart1.graphicsContext2D)
  }

  rootGrid.onMouseMoved = (event: MouseEvent) => {
    nearest = findClosest(event.x, event.y)
    renderChart(chart1.graphicsContext2D)
  }

  rootGrid.onKeyPressed = (event: KeyEvent) => {
    if event.metaDown || event.isControlDown then
      event.text match
        case "f" =>
          nearest.foreach(x => Future(
            try
              val time = scraperPlus.fetch(x.raceNum, userName).takeRight(8)
              x.date.set(Calendar.HOUR_OF_DAY, time.take(2).toInt)
              x.date.set(Calendar.MINUTE, time.slice(3, 5).toInt)
              x.date.set(Calendar.SECOND, time.takeRight(2).toInt)
              println(x.date.getTime.toString)
            catch
              case _ =>
                x.date.set(Calendar.HOUR_OF_DAY, 12)
                x.date.set(Calendar.MINUTE, 0)
                x.date.set(Calendar.SECOND, 0)
                println("Update Failed   " + x.date.getTime.toString)
            finally
              renderChart(chart1.graphicsContext2D)
          ))
          println("update")
        case "0" =>
          xZoom = 1.0
          yZoom = 1.0
          xMove = 0.0
          yMove = 0.0
          renderChart(chart1.graphicsContext2D)
        case _@x => println(x)
    else
      event.text match
        case "f" =>
          println("f")
          renderChart(chart1.graphicsContext2D)
        case "h" => stage.hide()
        case "s" => stage.show()
        case _ =>
  }

  rootGrid.width.onChange { (_, old, newWidth) =>
    UIx = newWidth.intValue()
    chart1.width = newWidth.intValue()
    renderChart(chart1.graphicsContext2D)
  }

  rootGrid.height.onChange { (_, old, newHeight) =>
    UIy = newHeight.intValue()
    chart1.height = newHeight.intValue()
    renderChart(chart1.graphicsContext2D)
  }

////////////////////  helper functions  ////////////////////

  private def fetchData(update: Boolean): Unit =

    if update then scraper(userName , -1, true, true)

    val path = "./UIsaves/"
    val filename = "UIdefaultSave.csv"
    val file = Source.fromFile("" + path + /*"\\" +*/ filename.takeWhile(_ != '.') + ".csv")
    val updateFile = file.getLines().toVector

    dataRows = updateFile.drop(1).filterNot(_.isBlank).map(line =>
      DataRow(
        line.takeWhile(_ != ';').strip().toInt,
        line.dropWhile(_ != ';').drop(1).takeWhile(_ != ';').strip().toInt,
        line.dropWhile(_ != ';').drop(1).dropWhile(_ != ';').drop(1).takeWhile(_ != ';').strip().toDouble,
        {
          val date = line.dropWhile(_ != ';').drop(1).dropWhile(_ != ';').drop(1).dropWhile(_ != ';').drop(1).strip()
          try
            val datef = (date.take(2).filterNot(_ == '.').toInt, date.dropRight(5).takeRight(2).filterNot(_ == '.').toInt, date.takeRight(4).toInt)
            new GregorianCalendar() {
              set(datef._3, datef._2 - 1, datef._1, 12, 0, 0)
            }
          catch
            case _@x =>
              println("Date was formatted incorrectly savefile")
              throw x
        }
      )
    ).sortBy(_.raceNum)
    lastRaceTime          = dataRows.last.date
    firstRaceTime         = dataRows.head.date
    timespan              = (lastRaceTime.getTimeInMillis - firstRaceTime.getTimeInMillis) / 1000
    firstRaceTimeInMillis = firstRaceTime.getTimeInMillis
    lastRaceNum           = dataRows.last.raceNum

    val WPMlist = dataRows.map(line => (line.WPM, line.raceNum))

    maxWPM = WPMlist.maxBy(_._1)._1
    minWPM = WPMlist.minBy(_._1)._1


  private def renderChart(c: GraphicsContext): Unit =

    if loadingFinished then
      val startTime: Long       = System.nanoTime()
      val xPoints: Double       = UIx.toDouble / timespan.toDouble * xZoom
      val yPoints: Double       = UIy.toDouble / ySteps * yZoom
      val wpmBallSize: Double   = math.pow(yPoints, 0.6) / 2 + 0.5
      val wBS2: Double          = wpmBallSize / 2
      val col10: Double         = math.min(0.14 + yPoints / 500, 0.22)

      def panX: Double = xMove
      def panY: Double = yMove
      def zoomX: Double = -UIx * (xZoom - 1) / 2
      def zoomY: Double = UIy + UIy * (yZoom - 1) / 2
      def getClosestNum(xPos: Double): Double = xPos / xPoints
      def newX(old: Double): Double = zoomX + old * xPoints + panX
      def newY(old: Double): Double = zoomY - old * yPoints + panY
      def ballCoords(x: Double, y: Double, size: Double): (Double, Double, Double, Double) = (x - size / 2, y - size / 2, x + size / 2, y + size / 2)

      c.fill = Color(0.13, 0.14, 0.15, 1)
      c.fillRect(0, 0, UIx, UIy)
      c.fill = Color(col10 - 0.01, col10, col10 + 0.01, 1)
      (0 until ySteps).foreach(y => c.fillRect(0, zoomY - y * yPoints + panY, UIx, 1))           // tarkemmat vaakalinjat
      c.fill = Color(0.21, 0.22, 0.23, 1)
      (0 until ySteps / 10).foreach(y => c.fillRect(0, zoomY - y * yPoints * 10 + panY, UIx, 1)) // epätarkemmat vaakalinjat

      ///// red balls which show wpm /////
      c.fill = DarkRed
      dataRows.foreach(line =>
        line.WPMlocaction(0) = zoomX + (line.date.getTimeInMillis - firstRaceTimeInMillis) / 1000 * xPoints - wBS2 + panX
        line.WPMlocaction(1) = zoomY - line.WPM * yPoints - wBS2 + panY
        c.fillOval(line.WPMlocaction.head, line.WPMlocaction(1), wpmBallSize, wpmBallSize)
      )


      ///// text formatting /////
      c.textAlign = TextAlignment(text.TextAlignment.CENTER)
      c.font = Font.font("Bauhaus 93", FontWeight.THIN, FontPosture.REGULAR, 15)

      nearest.foreach(n =>
        c.fill = Color(0.55, 0.56, 0.57, 0.5)
        c.fillRect(n.WPMlocaction(0) + wBS2 - 0.5, 0, 1, UIy)
        c.fillRect(0, n.WPMlocaction(1) + wBS2 - 0.5, UIx, 1)
        c.fill = White
        c.fillText(n.raceNum.toString, n.WPMlocaction(0), 20)
        c.fillText(n.WPM.toString, UIx - 20, n.WPMlocaction(1) + 5)
        cornerText.text =
          s"""Race:\t\t\t\t${n.raceNum}
             |WPM:\t\t\t\t${n.WPM}
             |Accuracy:\t\t\t${n.Acc}%
             |Date:\t\t\t\t${n.date.get(Calendar.DAY_OF_MONTH)}.${n.date.get(Calendar.MONTH) + 1}.${n.date.get(Calendar.YEAR)}
             |Time:\t\t\t\t${{"0" + n.date.get(Calendar.HOUR_OF_DAY)}.takeRight(2)}.${{"0" + n.date.get(Calendar.MINUTE)}.takeRight(2)}.${{"0" + n.date.get(Calendar.SECOND)}.takeRight(2)}""".stripMargin
      )

      c.fill = White

      if yPoints < 2.4 then
        (0 until ySteps / 20).foreach(y => c.fillText((y * 20).toString, 20, zoomY - y * yPoints * 20 + panY + 5))
        valTextY = yValueText / 2
      else if yPoints < 8 then
        valTextY = yValueText
        (0 until ySteps / 10).foreach(y => c.fillText((y * 10).toString, 20, zoomY - y * yPoints * 10 + panY + 5)) // vaakalinjojen teksti
      else if yPoints < 16 then
        valTextY = yValueText * 2
        (0 until ySteps / 5).foreach(y => c.fillText((y * 5).toString, 20, zoomY - y * yPoints * 5 + panY + 5))
      else if yPoints < 32 then
        (0 until ySteps / 2).foreach(y => c.fillText((y * 2).toString, 20, zoomY - y * yPoints * 2 + panY + 5))
        valTextY = yValueText * 4
      else
        (0 until ySteps).foreach(y => c.fillText(y.toString, 20, zoomY - y * yPoints + panY + 5))
        valTextY = yValueText * 8


      ///// dates at the bottom /////
      (dayLower to dayUpper).foreach(x => c.fillText({
        val scale = (UIx / valTextY) / 2
        if x == dayLower && newX(timespan / valTextY * x) >= scale then dayLower -= 1
        if x == dayLower && newX(timespan / valTextY * x) < scale then dayLower += 1
        if x == dayUpper && newX(timespan / valTextY * x) >= UIx + (UIx / valTextY) / 2 then dayUpper -= 1
        if x == dayUpper && newX(timespan / valTextY * x) < UIx - scale then dayUpper += 1


        val cal = new GregorianCalendar {
          setTimeInMillis((getClosestNum(x * (UIx / valTextY) + UIx / (valTextY * 2) - zoomX - panX) * 1000 + firstRaceTimeInMillis).round)
        }
        s"${cal.get(Calendar.DAY_OF_MONTH)}.${cal.get(Calendar.MONTH) + 1}.${cal.get(Calendar.YEAR)}"},
        newX(timespan / valTextY * x),
        UIy - 10))

    else
      println(loadingText)
      c.fill = Color(0.13, 0.14, 0.15, 1)
      c.fillRect(0, 0, UIx, UIy)
      c.fill = White
      c.textAlign = TextAlignment.Center
      c.fillText(loadingText, UIx / 2, UIy / 2)

  private def findClosest(x: Double, y: Double): Option[DataRow] =
    try
      var middle: Int = dataRows.size
      var rangeMax = dataRows.size - 1
      var rangeMin = 0

      while rangeMax - rangeMin > 10 do
        middle = (rangeMax + rangeMin) / 2
        if dataRows(middle).WPMlocaction.head < x then
          rangeMin = middle
        else if dataRows(middle).WPMlocaction.head > x then
          rangeMax = middle
        else
          rangeMax = rangeMin

      val dRowX = dataRows.slice(middle - 40, middle + 40).sortBy(row =>
          sqrt(
            (row.WPMlocaction.head - x) *
            (row.WPMlocaction.head - x) +
            (row.WPMlocaction(1) - y) *
            (row.WPMlocaction(1) - y)
          ))
      if sqrt(
        (dRowX.head.WPMlocaction.head - x) *
        (dRowX.head.WPMlocaction.head - x) +
        (dRowX.head.WPMlocaction(1) - y) *
        (dRowX.head.WPMlocaction(1) - y)) < 150
      then
        Some(dRowX.head) else None
    catch
      case _ => None







