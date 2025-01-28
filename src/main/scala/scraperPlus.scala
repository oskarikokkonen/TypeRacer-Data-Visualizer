import org.jsoup.Jsoup

import java.net.{URL, URLEncoder}
import java.nio.charset.StandardCharsets

object scraperPlus:
  def fetch(race: Int, usrName: String) =
    val encodedURL = "https://data.typeracer.com/pit/result?id=" + URLEncoder.encode(s"|tr:$usrName|$race", StandardCharsets.UTF_8)
    val dataALL = Jsoup.connect(encodedURL).maxBodySize(0).get().select("div.main > div.themeContent > div.section")

    val raceDAY = dataALL.select("tr:contains(Date)").text().dropWhile(_ != ',').drop(2).dropRight(6)
    println(raceDAY)
    raceDAY
