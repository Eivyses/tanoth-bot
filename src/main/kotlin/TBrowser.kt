package org.example

import io.github.bonigarcia.wdm.WebDriverManager
import java.time.Duration
import org.openqa.selenium.By
import org.openqa.selenium.Cookie
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.support.ui.ExpectedCondition
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait

class TBrowser : AutoCloseable {

  private lateinit var driver: WebDriver

  fun getNewSessionId(gfToken: String): String {
    use {
      val options = getBrowserOptions()
      driver = ChromeDriver(options)

      driver.get("https://lobby.tanoth.gameforge.com/en_GB/hub")
      addGfToken(gfToken)
      driver.navigate().refresh()

      val wait = WebDriverWait(driver, Duration.ofSeconds(10))
      val element =
          wait.until(ExpectedConditions.elementToBeClickable(By.className("button-default")))
      element.click()
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
          addArguments("--headless")
          addArguments("--disable-gpu")
          addArguments("--no-sandbox")
          addArguments("--disable-dev-shm-usage")
          addArguments("--mute-audio")
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
