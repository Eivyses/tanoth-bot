package org.example

import io.github.bonigarcia.wdm.WebDriverManager
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.Duration
import kotlinx.coroutines.delay
import org.openqa.selenium.By
import org.openqa.selenium.Cookie
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.support.ui.ExpectedCondition
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait

private val logger = KotlinLogging.logger {}

class TBrowser : AutoCloseable {

  private lateinit var driver: WebDriver

  suspend fun getNewSessionId(gfToken: String): String {
    use {
      val options = getBrowserOptions()
      driver = ChromeDriver(options)

      driver.get("https://lobby.tanoth.gameforge.com/en_GB/hub")
      addGfToken(gfToken)
      driver.navigate().refresh()

      val wait = WebDriverWait(driver, Duration.ofSeconds(10))
      // wait for normal PLAY button to be visible
      wait.until(ExpectedConditions.elementToBeClickable(By.className("button-primary")))
      var buttonsList = driver.findElements(By.className("button-default"))
      // if last played button is not there then it means that server is in maintenance mode
      while (buttonsList.isEmpty()) {
        logger.warn { "Server in maintenance mode, waiting..." }
        delay(300_000)
        driver.navigate().refresh()
        wait.until(ExpectedConditions.elementToBeClickable(By.className("button-primary")))
        buttonsList = driver.findElements(By.className("button-default"))
      }
      buttonsList.first().click()
      // game opens in new tab
      driver.switchTo().window(driver.windowHandles.last())
      // takes a while to load, flashvars is a good indication when it's ready
      wait.until(
          ExpectedCondition {
            (driver as JavascriptExecutor).executeScript(
                "return typeof flashvars !== 'undefined' && flashvars.sessionID != null") as Boolean
          })

      val source = driver.pageSource!!
      return parseSessionIdFromHtml(source)
    }
  }

  private fun getBrowserOptions(): ChromeOptions {
    WebDriverManager.chromedriver().setup()

    val options =
        ChromeOptions().apply {
          addArguments("--headless=new")
          addArguments("--disable-gpu")
          addArguments("--no-sandbox")
          addArguments("--disable-dev-shm-usage")
          addArguments("--mute-audio")

          // headless performance options
          addArguments("--window-size=1920,1080")
          addArguments("--window-position=0,0")
          addArguments("--proxy-server='direct://'")
          addArguments("--proxy-bypass-list=*")

          // logs: 0 = ALL, 1 = INFO, 2 = WARNING, 3 = ERROR
          addArguments("--log-level=3")

          // anti bot options
          addArguments("--disable-blink-features=AutomationControlled")
          addArguments("--disable-features=UserAgentClientHint")
          addArguments("--disable-logging")
        }
    return options
  }

  private fun addGfToken(token: String) {
    val cookie =
        Cookie.Builder("gf-token-production", token)
            .domain(".gameforge.com")
            .path("/")
            .isHttpOnly(false)
            .isSecure(false)
            .build()
    driver.manage().addCookie(cookie)
  }

  override fun close() {
    driver.quit()
  }
}
