import org.jsoup.nodes.Document
import org.jsoup.{HttpStatusException, Jsoup}
import scalafx.scene.input.DataFormat.Files

import java.awt.Desktop
import java.time
import scala.collection.mutable
import java.io.{File, PrintWriter}
import java.net.http.HttpConnectTimeoutException
import java.time.Year
import scala.annotation.tailrec
import scala.io.StdIn.readLine
import scala.collection.mutable.Buffer
import scala.io.Source
import scala.util.boundary.break

def scraper(userName: String, amount: Int = - 1, updateMode: Boolean, UImode: Boolean = false): Unit =

  var update = updateMode
  var path = ""
  var filename = ""

  if !UImode then
    path = readLine(s"Enter path to folder ${if !update then "where CVS will be saved:" else "which includes CVS file"}   (default is ./)\n")
    if path.isEmpty then path = "./"
    filename = readLine(if !update then "Enter filename: (default is \"test\")\n" else "Enter the name of the CVS file that will be updated: \n")
    if filename.isEmpty then filename = "default"
  else
    path = "./UIsaves/"
    filename = "UIdefaultSave.csv"

  if !File("" + path + /*"\\" +*/ filename.takeWhile(_ != '.') + ".csv" ).exists() then update = false
  var updateFile = Vector[String]()

  var finAmount: Int =
    try
      if amount <= - 1 && !update then
          Jsoup.connect(s"https://data.typeracer.com/pit/race_history?user=$userName&n=1").get().select("div.profileTableHeaderUniverse").text().toInt
      else if update then
        val file = Source.fromFile("" + path +/* "\\" +*/ filename.takeWhile(_ != '.') + ".csv" )
        updateFile = file.getLines().toVector
        math.max(Jsoup.connect(s"https://data.typeracer.com/pit/race_history?user=$userName&n=1").get().select("div.profileTableHeaderUniverse").text().toInt - updateFile(1).takeWhile(_ != ';').strip().toInt, 0)
      else amount
    catch
      case n: NumberFormatException =>
        println("Username " + userName + " wasn't found, try again")
        System.exit(0)
        0
      case _@x => throw x

  val finNUM = Buffer[Int]()
  val finWPM = Buffer[Int]()
  val finACC = Buffer[Double]()
  val finDAY = Buffer[(Int, Int, Int)]()
  val blockSize = 500


  var weburl = s"https://data.typeracer.com/pit/race_history?user=$userName&n=${math.min(finAmount, blockSize)}"
  var passes = 0

  try
    while finAmount > 0 do
      println("Races fetched: " + finNUM.length + ", remaining: " + finAmount)
      var delay = 0
      if delay > 0 then
        println("Typeracer server reducing speed, latency: " + delay + " ms")
        Thread.sleep(delay)
        delay = math.max(delay - 25, 0)

      if passes >= 1 then
        val lastD = dateAdder(finDAY.last)
        weburl = s"https://data.typeracer.com/pit/race_history?user=$userName&n=$blockSize&startDate=${lastD._3}-${lastD._2}-${lastD._1}&universe="
//      println(weburl)

      val dataALL =
        @tailrec def fetch(times: Int): Document =
          try
            Jsoup.connect(weburl).timeout(600000).get()
          catch
            case x: HttpStatusException =>
              if times == 1 then delay += 200
              if times < 10 then
                val waitTime = (math.pow(times / 3.0, 2.5) * 500).toInt + 500
                println("Server not responding, waiting " + waitTime + " ms")
                Thread.sleep(waitTime)
                fetch(times + 1)
              else throw HttpConnectTimeoutException("Server not responding")
            case _@x => throw x
        fetch(1)
      val dataNUM = dataALL.select("div.profileTableHeaderUniverse").text()
      val dataWPM = dataALL.select("div.profileTableHeaderRaces:contains(WPM)").text()
      val dataACC = dataALL.select("div.profileTableHeaderRaces:contains(%)").text()
      var dataDAY = dataALL.select("div.profileTableHeaderDate").text()
      val dataNUMf = dataNUM.split(' ').map(_.toInt)
      val dataWPMf = dataWPM.filterNot(_ == ' ').split("WPM").map(_.toInt)
      val dataACCf = dataACC.split(' ').map(_.dropRight(1).toDouble)
      val dataDAYf = mutable.Buffer[(Int, Int, Int)]()
      while dataDAY.nonEmpty do
        val take = dataDAY.take(5)
        if take == "today" then
          dataDAYf += ((time.MonthDay.now().getDayOfMonth, time.YearMonth.now().getMonthValue, time.Year.now().getValue))
          dataDAY = dataDAY.dropWhile(_ != ' ').tail
        else
          val MD = dataDAY.takeWhile(_ != ',')
          val Year = dataDAY.drop(MD.length + 2).takeWhile(_ != ' ')
          val str = s"$MD, $Year"
          dataDAYf += dateConverter(str.strip())
          dataDAY = dataDAY.drop(str.length)

      val dropAmount = if finNUM.nonEmpty then dataNUMf.takeWhile(_ >= finNUM.last).length else 0
      finNUM ++= dataNUMf.drop(dropAmount)
      finWPM ++= dataWPMf.drop(dropAmount)
      finACC ++= dataACCf.drop(dropAmount)
      finDAY ++= dataDAYf.drop(dropAmount)
      finAmount = if finAmount > 1 && dropAmount < dataNUMf.length then math.max(finAmount - dataNUMf.length + dropAmount, 0) else 0
      passes += 1
      if finNUM.length > amount && amount != -1 then
        val amt = finNUM.length - amount
        finNUM.dropRightInPlace(amt)
        finWPM.dropRightInPlace(amt)
        finACC.dropRightInPlace(amt)
        finDAY.dropRightInPlace(amt)
        finAmount = 0

    println("Races fetched: " + finNUM.length + ", remaining: " + finAmount)
    println("Fetching completed")
    println("Saving file")
    if finNUM.nonEmpty then
      val connected =
        for x <- finWPM.indices yield
          s"${finNUM(x)};${finWPM(x)};${finACC(x)};${finDAY(x).toString.drop(1).dropRight(1).map(x => if x == ',' then '.' else x)}"


      val writer = new PrintWriter("" + path + /*"\\" +*/ filename.takeWhile(_ != '.') + ".csv" )
      if !update then
        writer.write("RaceNum; WPM; Accuracy; Date;\n" + connected.mkString("\n"))
      else
        writer.write("RaceNum; WPM; Accuracy; Date;\n" + connected.mkString("\n") + "\n" + updateFile.drop(1).mkString("\n"))
      writer.close()

      if !UImode then Desktop.getDesktop.open(new File(path))

    println("File saved to " + File(path).getPath)
  catch
    case n: NumberFormatException =>
      println("Username " + userName + " wasn't found, try again")
    case _@x =>
      println("Terminating fetcher due to error: " + x)







