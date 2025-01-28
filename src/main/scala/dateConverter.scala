def dateConverter(str: String): (Int, Int, Int) = // day, month, year
  val monthString = str.takeWhile(_ != ' ')
  val dayString = str.drop(monthString.length + 1).takeWhile(_ != ',')
  val yearString = str.dropWhile(_ != ',').drop(2)

  val dayNUM = dayString.toInt
  val yearNUM = yearString.toInt
  val monthNUM =
    monthString match
      case "Jan." => 1
      case "Feb." => 2
      case "March" => 3
      case "April" => 4
      case "May" => 5
      case "June" => 6
      case "July" => 7
      case "Aug." => 8
      case "Sept." => 9
      case "Oct." => 10
      case "Nov." => 11
      case "Dec." => 12
      case _@x => throw NumberFormatException(s"Month $x wasn't found")

  (dayNUM, monthNUM, yearNUM)

