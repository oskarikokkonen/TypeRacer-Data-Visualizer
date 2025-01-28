
def dateAdder(date: (Int, Int, Int)): (Int, Int, Int) =
  var lastD = date
  if lastD._1 >= 28 then
    lastD._2 match
      case 1 => if lastD._1 < 31 then lastD = (lastD._1 + 1, lastD._2, lastD._3) else lastD = (1, lastD._2 + 1, lastD._3)
      case 2 => lastD = (1, 3, lastD._3)
      case 3 => if lastD._1 < 31 then lastD = (lastD._1 + 1, lastD._2, lastD._3) else lastD = (1, lastD._2 + 1, lastD._3)
      case 4 => if lastD._1 < 30 then lastD = (lastD._1 + 1, lastD._2, lastD._3) else lastD = (1, lastD._2 + 1, lastD._3)
      case 5 => if lastD._1 < 31 then lastD = (lastD._1 + 1, lastD._2, lastD._3) else lastD = (1, lastD._2 + 1, lastD._3)
      case 6 => if lastD._1 < 30 then lastD = (lastD._1 + 1, lastD._2, lastD._3) else lastD = (1, lastD._2 + 1, lastD._3)
      case 7 => if lastD._1 < 31 then lastD = (lastD._1 + 1, lastD._2, lastD._3) else lastD = (1, lastD._2 + 1, lastD._3)
      case 8 => if lastD._1 < 31 then lastD = (lastD._1 + 1, lastD._2, lastD._3) else lastD = (1, lastD._2 + 1, lastD._3)
      case 9 => if lastD._1 < 30 then lastD = (lastD._1 + 1, lastD._2, lastD._3) else lastD = (1, lastD._2 + 1, lastD._3)
      case 10 => if lastD._1 < 31 then lastD = (lastD._1 + 1, lastD._2, lastD._3) else lastD = (1, lastD._2 + 1, lastD._3)
      case 11 => if lastD._1 < 30 then lastD = (lastD._1 + 1, lastD._2, lastD._3) else lastD = (1, lastD._2 + 1, lastD._3)
      case 12 => if lastD._1 < 31 then lastD = (lastD._1 + 1, lastD._2, lastD._3) else lastD = (1, 1, lastD._3 + 1)
  else
    lastD = (lastD._1 + 1, lastD._2, lastD._3)
  lastD
