import scalafx.scene.canvas.GraphicsContext
import scalafx.scene.paint.Color
import scalafx.scene.paint.Color.White
import scalafx.scene.text.TextAlignment

class LoadAnimation(c: GraphicsContext) extends Thread:
  var animate: Boolean    = true
  var loadingText: String = "Loading data..."
  override def run(): Unit =
    println("Thread Started")
    while animate do
      println(loadingText)
      if loadingText.takeRight(3) == "..." then
        loadingText = "Loading data"
      else if loadingText.takeRight(2) == ".." then
        loadingText = "Loading data..."
      else if loadingText.takeRight(1) == "." then
        loadingText = "Loading data.."
      else
        loadingText = "Loading data."
      println("rendered :" + loadingText)
      //      println(loadingText)
      c.fill = Color(0.13, 0.14, 0.15, 1)
      c.fillRect(0, 0, UIx, UIy)
      c.fill = White
      c.textAlign = TextAlignment.Center
      c.fillText(loadingText, UIx / 2, UIy / 2)
      Thread.sleep(500)
      
  def stopAnimation(): Unit = 
    animate = false

