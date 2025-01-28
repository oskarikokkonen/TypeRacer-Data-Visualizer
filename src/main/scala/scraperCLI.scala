import scala.annotation.tailrec
import scala.io.StdIn.readLine

object scraperCLI:
  def main(args: Array[String]): Unit =
    val username: String = LazyList.continually(readLine("Enter Typeracer username: ")).dropWhile(_.isEmpty).head
    val updateMode: Boolean = LazyList.continually(readLine("(N)ew File,  (U)pdate old File: ")).dropWhile(x => x.isEmpty || (x.head.toLower != 'u' && x.head.toLower != 'n')).map(x => if x.head == 'u' then true else false).head
    val amount: Int =
      if !updateMode then
        @tailrec def inner: Int =
          try
            val input = readLine("Number of races to be fetched, leave empty for all: ")
            if input.isEmpty then -1 else input.toInt
          catch
            case n: NumberFormatException =>
              println("Enter only numbers")
              inner
            case _@x =>
              println(x)
              throw x
        inner
      else
        -1

    scraper(username, amount, updateMode)


