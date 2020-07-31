package be.unamur.ct.scrap.service;

import be.unamur.ct.scrap.model.PageVisit;
import be.unamur.ct.scrap.model.SiteVisit;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import lombok.SneakyThrows;
import org.assertj.core.util.Lists;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.slf4j.LoggerFactory.getLogger;

class VatScraperTest {

  private final PageFetcher pageFetcher = new PageFetcher(PageFetcherConfig.defaultConfig());
  private final VatFinder vatFinder = new VatFinder();
  private final VatScraper scraper = new VatScraper(pageFetcher, vatFinder);
  private static final Logger logger = getLogger(VatScraperTest.class);

  public WireMockRule wireMockRule = new WireMockRule(options().dynamicPort());

  @BeforeEach
  public void setup() throws IOException {
    //  Creating small test web server to serve web pages to scrap
    stubFor("/", "html/simple/index.html");
    stubFor("/simple/coeck.html", "html/simple/coeck.html");
    for (String s : new String[] {"index", "page-1", "page-2"}) {
      stubFor("/complex/" + s, "html/complex/" + s + ".html");
    }
    for (String s : new String[] {"wrong-checksum", "wrong-format"}) {
      stubFor("/wrong/" + s, "html/wrong/" + s + ".html");
    }
    wireMockRule.start();
  }

  private void stubFor(String url, String resourcePath) throws IOException {
    byte[] body = FileCopyUtils.copyToByteArray( new ClassPathResource(resourcePath).getInputStream());
    logger.debug("Setting up stub content for url={} => {}", url, resourcePath);
    wireMockRule.stubFor(get(urlEqualTo(url))
        .willReturn(aResponse()
            .withBody(body)
            .withHeader("Content-Type", "text/html; charset=UTF-8"))
    );
  }

  @Test
  void vatFoundOnFirstPage() {
    // Easy case: VAT on first page
    SiteVisit visit = scraper.visit(urlFor("/"), 1);
    String vat = visit.getVat();
    logger.info("vat = {}", vat);
    assertThat(vat).isEqualTo("BE0542703815");
  }

  @Test
  void frames() {
    // /simple/coeck.html
    PageVisit page = scraper.fetchAndParse(urlFor("/simple/coeck.html"));
    logger.info("page = {}", page);
    //logger.info("page = {}", page.getBody());
    logger.info("doc = {}", page.getDocument().text());
  }

  @Test
  void coeck() throws MalformedURLException {
    // /simple/coeck.html
    URL url = new URL("http://coeck-centraleverwarming.be/");
    PageVisit page = scraper.fetchAndParse(url);
    logger.info("page = {}", page);
    //logger.info("page = {}", page.getBody());
    logger.info("doc = {}", page.getDocument().text());

    List<String> links = scraper.findInnerLinks(url, page.getDocument());
    logger.info("links = {}", links);

  }

  @Test
  void noVatOnFirstPage() {
    // VAT not on first page and depth too small
    SiteVisit visit = scraper.visit(urlFor("/complex/index"), 1);
    String vat = visit.getVat();
    logger.info("vat = {}", vat);
    assertThat(vat).isNull();
    assertThat(visit.getVisitedURLs().size()).isEqualTo(1);
  }

  @Test
  void vatFoundOnSecondPage() {
    SiteVisit visit = scraper.visit(urlFor("/complex/index"), 2);
    String vat = visit.getVat();
    logger.info("vat = {}", vat);
    // VAT not on first page and depth large enough to find it on second page
    assertThat(vat).isEqualTo("BE0542703815");
    assertThat(visit.getVisitedURLs().size()).isEqualTo(2);
  }

  @Test
  public void wrongFormat() {
    // VAT not found because of wrong format
    SiteVisit visit = scraper.visit(urlFor("/wrong/wrong-format"), 2);
    String vat = visit.getVat();
    logger.info("vat = {}", vat);
    // VAT not on first page and depth large enough to find it on second page
    assertThat(vat).isNull();
    assertThat(visit.getVisitedURLs().size()).isEqualTo(1);
  }

  @Test
  public void wrongCheckSum() {
    SiteVisit visit = scraper.visit(urlFor("/wrong/wrong-checksum"), 2);
    String vat = visit.getVat();
    logger.info("wrong checksum => vat = {}", vat);
    assertThat(vat).isNull();
  }

  @SneakyThrows
  private URL urlFor(String path) {
    return new URL("http://localhost:" + wireMockRule.port() + path);
  }

  @Test
  public void printMappings() throws IOException {
    logger.info("wireMockRule.port(); = {}", wireMockRule.port());
    PageVisit page = pageFetcher.fetch(urlFor("/"));
    logger.info("root = {}", page);
    logger.info("root body = {}", page.getBody());
    page = pageFetcher.fetch(urlFor("/__admin"));
    logger.info("__admin = {}", page);
    logger.info("__admin body = {}", page.getBody());
  }


  @Test
  void getSecondLevelDomainName() throws MalformedURLException {
    assertThat(scraper.getSecondLevelDomainName("http://www.example.com")).isEqualTo("example.com");
    assertThat(scraper.getSecondLevelDomainName("http://x.y.z.example.com?some/path?withQ=param")).isEqualTo("example.com");
    URL url = URI.create("http://10.20.30.40/and/some/path?withQuery=params").toURL();
    String dn = scraper.getSecondLevelDomainName(url);
    logger.info("{} => {}", url, dn);
    assertThat(dn).isEqualTo("30.40");
  }

  @Test
  public void findInnerLinks() throws IOException {
    URL url = new URL("http://www.dnsbelgium.be/nl/");
    Document document = Jsoup.parse(url, 5000);
    List<String> links = scraper.findInnerLinks(url, document);
    logger.info("links = {}", links.size());
    for (String link : links) {
      logger.info("link = {}", link);
    }
  }

  @Test
  public void findVatOnContactPage() throws IOException {
    URL url = new URL("https://www.dnsbelgium.be/en/contact");
    Document document = Jsoup.parse(url, 5000);
    String body = document.body().text();
    List<String> values = vatFinder.findVatValues(body);
    logger.info("VAT values = {}", values);
    assertThat(values).contains("BE0466158640");
  }

  @Test
  public void visitDnsBelgium() throws MalformedURLException {
    scrapeForReal("https://www.dnsbelgium.be/en/");
  }

  @Test
  public void sortLinks() {
    String contactLink = "https://www.dnsbelgium.be/en/contact";
    String aboutLink = "https://www.dnsbelgium.be/about/";

    List<String> links = Lists.newArrayList(
        "https://www.dnsbelgium.be/",
        "https://www.dnsbelgium.be/register/",
        "https://www.dnsbelgium.be/regitser/",
        aboutLink,
        "https://www.dnsbelgium.be/register/your-domain",
        contactLink,
        "https://www.dnsbelgium.be/register/abc"
    );
    links.sort(scraper::sortLinks);
    logger.info("sorted links = {}", links);
    assertThat(links.get(0)).withFailMessage("about link should be first").isEqualTo(aboutLink);
    assertThat(links.get(1)).withFailMessage("contact link should be second").isEqualTo(contactLink);

  }

  @Test
  public void testNPE() throws MalformedURLException {
    scrapeForReal("http://coeck-centraleverwarming.be/");
  }

  private void scrapeForReal(String link) throws MalformedURLException {
    URL url = new URL(link);
    SiteVisit visit = scraper.visit(url, 45);
    for (PageVisit pageVisit : visit.getPageVisitList()) {
      logger.info("we visited {}", pageVisit.getUrl());
    }
    logger.info("visit = {}", visit);
  }

  @Test
  public void pkixPathBuildingFailed() throws MalformedURLException {
    // leads to "PKIX path building failed"
    // Failed to fetch http://chezye.be beacuse of PKIX path building failed: sun.security.provider.certpath.SunCertPathBuilderException: unable to find valid certification path to requested target
    // should contain
    // <p class="fw-special-subtitle">BE 0598.694.886</p>  ??
    scrapeForReal("https://www.chezye.be/");
  }

}