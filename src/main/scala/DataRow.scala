import java.util.GregorianCalendar

case class DataRow(raceNum: Int, WPM: Int, Acc: Double, date: GregorianCalendar):

  var WPMlocaction: Array[Double] = Array(0.0, 0.0)

  var additionalData: Option[AdditionalData] = None

class AdditionalData