package be.unamur.ct.scrap.service;


import be.unamur.ct.scrap.model.PageVisit;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.slf4j.LoggerFactory.getLogger;

class PageFetcherTest {

  private final PageFetcher pageFetcher = new PageFetcher(PageFetcherConfig.defaultConfig());
  private static final Logger logger = getLogger(PageFetcherTest.class);

  @Test
  public void delay() throws IOException {
    PageVisit pageVisit = pageFetcher.fetch("http://httpbin.org/delay/0.5");
    logger.info("pageVisit = {}", pageVisit.getVisitDuration());
    assertThat(pageVisit.getVisitDuration().toMillis()).isBetween(500L, 2000L);
  }

  @Test
  @DisplayName("http://www.google.com/")
  public void httpGoogle() throws IOException {
    PageVisit pageVisit = pageFetcher.fetch("http://www.google.com/");
    logger.info("pageVisit = {}", pageVisit);
    assertThat(pageVisit.getStatusCode()).isEqualTo(200);
    assertThat(pageVisit.getProtocol()).isEqualTo("http/1.1");
    assertThat(pageVisit.getTlsVersion()).isBlank();
    assertThat(pageVisit.getCipherSuite()).isBlank();
    assertThat(pageVisit.getBody()).isNotNull();
    assertThat(pageVisit.getBody().length()).isBetween(100, 50_000);
  }

  @Test
  @DisplayName("https://www.google.com/")
  public void httpsGoogle() throws IOException {
    PageVisit pageVisit = pageFetcher.fetch("https://www.google.com/");
    logger.info("pageVisit = {}", pageVisit);
    assertThat(pageVisit.getStatusCode()).isEqualTo(200);
    assertThat(pageVisit.getProtocol()).isEqualTo("http/1.1");
    assertThat(pageVisit.getTlsVersion()).isEqualTo("TLSv1.3");
    assertThat(pageVisit.getCipherSuite()).contains("TLS");
    assertThat(pageVisit.getBody()).isNotNull();
    assertThat(pageVisit.getBody().length()).isBetween(100, 50_000);
  }

  @Test
  @DisplayName("https://www.dnsbelgium.com/")
  public void https_dnsbelgium_dot_com() throws IOException {
    // fetch will fail because of Received fatal alert: unrecognized_name
    PageVisit pageVisit = pageFetcher.fetch("https://www.dnsbelgium.com/");
    logger.info("pageVisit = {}", pageVisit);
    assertThat(pageVisit.getStatusCode()).isEqualTo(0);
  }

  @Test
  @Timeout(value = 1, unit = TimeUnit.MINUTES)
  @DisplayName("https://www.dnsbelgium.be/")
  public void https_dnsbelgium__dot_be() throws IOException {
    URL url = new URL("https://www.dnsbelgium.be/");
    PageVisit pageVisit = pageFetcher.fetch(url);
    logger.info("pageVisit = {}", pageVisit);
    assertThat(pageVisit.getUrl()).isEqualTo(url);
    assertThat(pageVisit.getStatusCode()).isEqualTo(200);
    assertThat(pageVisit.getVisitDuration()).isLessThan(Duration.ofSeconds(10));
    assertThat(pageVisit.getProtocol()).isEqualTo("http/1.1");
    assertThat(pageVisit.getTlsVersion()).isEqualTo("TLSv1.3");
    assertThat(pageVisit.getCipherSuite()).contains("TLS");
    assertThat(pageVisit.getBody()).isNotNull();
    assertThat(pageVisit.getBody().length()).isBetween(100, 50_000);
  }

  @Test
  @Disabled("Test disabled since it depends on state of chezye.be")
  public void pkixPath() throws IOException {
    // https://www.ssllabs.com/ssltest/analyze.html?d=www.chezye.be&hideResults=on
    // reports an incomplete certifcate chain

    // https://observatory.mozilla.org/analyze/www.chezye.be
    // but works in Chrome ...

    PageVisit pageVisit = pageFetcher.fetch("https://www.chezye.be/");
    logger.info("pageVisit = {}", pageVisit);
    assertThat(pageVisit.getStatusCode()).isEqualTo(0);
    assertThat(pageVisit.getBody().length()).isEqualTo(0);
  }

  @Test
  public void close() throws IOException {
    PageVisit pageVisit = pageFetcher.fetch("http://www.google.com/");
    assertThat(pageVisit.getStatusCode()).isEqualTo(200);
    pageFetcher.close();
    // a fetch call will now throw an IllegalStateException
    assertThrows(IllegalStateException.class,
        () -> pageFetcher.fetch("http://www.google.com/"));
  }

}