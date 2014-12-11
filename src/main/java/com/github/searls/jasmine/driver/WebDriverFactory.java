package com.github.searls.jasmine.driver;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.plexus.util.StringUtils;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.remote.BrowserType;
import org.openqa.selenium.remote.DesiredCapabilities;

import com.github.searls.jasmine.mojo.Capability;
import com.google.common.base.Objects;

import edu.emory.mathcs.backport.java.util.Arrays;

/**
 * Creates a WebDriver for TestMojo using configured properties.
 */
public class WebDriverFactory {
  private boolean debug;
  private String browserVersion;
  private String webDriverClassName;
  private List<Capability> webDriverCapabilities;
  private List<String> chromeDriverCommandLineArgs = new ArrayList<String>();

  public WebDriverFactory() {
    setWebDriverCapabilities(null);
  }

  public void setDebug(boolean debug) {
    this.debug = debug;
  }

  public void setBrowserVersion(String browserVersion) {
    this.browserVersion = browserVersion;
  }

  public void setWebDriverClassName(String webDriverClassName) {
    this.webDriverClassName = webDriverClassName;
  }

  public void setWebDriverCapabilities(List<Capability> webDriverCapabilities) {
    this.webDriverCapabilities = Objects.firstNonNull(webDriverCapabilities, Collections.<Capability>emptyList());
  }

  /**
   * Splits string into list of strings
   * @param commandLineArgs
   */
  public void setChromeDriverCommandLineArgs(String commandLineArgs) {
    // might want to add trim
    this.chromeDriverCommandLineArgs = new ArrayList<String>(Arrays.asList(commandLineArgs.split(",")));

  }

  public WebDriver createWebDriver() throws Exception {
    if (HtmlUnitDriver.class.getName().equals(webDriverClassName)) {
      return createDefaultWebDriver();
    } else {
      return createCustomWebDriver();
    }
  }

  @SuppressWarnings("unchecked")
  private Class<? extends WebDriver> getWebDriverClass() throws Exception {
    return (Class<WebDriver>) Class.forName(webDriverClassName);
  }

  private Constructor<? extends WebDriver> getWebDriverConstructor() throws Exception {
    Class<? extends WebDriver> webDriverClass = getWebDriverClass();
    boolean hasCapabilities = !webDriverCapabilities.isEmpty() || !chromeDriverCommandLineArgs.isEmpty();
    try {
      if (hasCapabilities) {
        return webDriverClass.getConstructor(Capabilities.class);
      }
      return webDriverClass.getConstructor();
    } catch (Exception exception) {
      if (hasCapabilities) {
        return webDriverClass.getConstructor();
      }
      return webDriverClass.getConstructor(Capabilities.class);
    }
  }

  private WebDriver createCustomWebDriver() throws Exception {
    Constructor<? extends WebDriver> constructor = getWebDriverConstructor();
    return constructor.newInstance(getWebDriverConstructorArguments(constructor));
  }

  private Object[] getWebDriverConstructorArguments(Constructor<? extends WebDriver> constructor) {

    DesiredCapabilities capabilities = getCapabilities();
    // if the web driver is chromedriver, and if there actually are command line arguments
    if (ChromeDriver.class.getName().equals(webDriverClassName) && !chromeDriverCommandLineArgs.isEmpty()) {
      Map<String, Object> chromeOptions = new HashMap<String, Object>();
      chromeOptions.put("args", chromeDriverCommandLineArgs);
      capabilities.setCapability(ChromeOptions.CAPABILITY, chromeOptions);
    }

    if (constructor.getParameterTypes().length == 0) {
      return new Object[0];
    }
    return new Object[] {capabilities};
  }

  private DesiredCapabilities getCapabilities() {
    DesiredCapabilities capabilities = new DesiredCapabilities();
    capabilities.setJavascriptEnabled(true);

    for (Capability capability : webDriverCapabilities) {
      Object value = capability.getValue();
      if (value != null && (!String.class.isInstance(value) || StringUtils.isNotBlank((String)value))) {
        capabilities.setCapability(capability.getName(),capability.getValue());
      } else if (capability.getList() != null && !capability.getList().isEmpty()) {
        capabilities.setCapability(capability.getName(),capability.getList());
      } else if (capability.getMap() != null && !capability.getMap().isEmpty()) {
        capabilities.setCapability(capability.getName(),capability.getMap());
      }
    }
    return capabilities;
  }

  private WebDriver createDefaultWebDriver() throws Exception {
    DesiredCapabilities capabilities = getCapabilities();
    if (StringUtils.isBlank(capabilities.getBrowserName())) {
      capabilities.setBrowserName(BrowserType.HTMLUNIT);
    }
    if (StringUtils.isBlank(capabilities.getVersion())) {
      capabilities.setVersion(browserVersion.replaceAll("(\\D+)_(\\d.*)?", "$1-$2").replaceAll("_", " ").toLowerCase());
    }
    return new QuietHtmlUnitDriver(getCapabilities(), debug);
  }
}
