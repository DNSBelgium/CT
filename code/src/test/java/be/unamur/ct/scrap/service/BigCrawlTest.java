package be.unamur.ct.scrap.service;

import be.unamur.ct.data.dao.CertificateDao;
import be.unamur.ct.data.dao.FoundVatRepository;
import be.unamur.ct.data.dao.FoundVatRepositoryTest;
import be.unamur.ct.decode.model.Certificate;
import be.unamur.ct.scrap.model.FoundVat;
import be.unamur.ct.scrap.model.SiteVisit;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.test.annotation.Commit;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.slf4j.LoggerFactory.getLogger;

@SpringBootTest
@Disabled("Only trigger big crawls manually")
public class BigCrawlTest {

  @Autowired private FoundVatRepository foundVatRepository;

  @Autowired private VatScraper vatScraper;

  @Autowired private CertificateDao certificateDao;

  private static final Logger logger = getLogger(FoundVatRepositoryTest.class);

  private final AtomicInteger crawled = new AtomicInteger(0);

  private final static int MAX_CRAWLS = Integer.MAX_VALUE;

  private void crawl(String subject)  {
    String preUrl = "http://" + (subject.startsWith("*.") ? "www." + subject.substring(2) : subject);
    try {
      URL url = new URL(preUrl);
      String domainName = getSecondLevelDomainName(subject);
      SiteVisit siteVisit = vatScraper.visit(url, 50);

      if (siteVisit.isVatFound()) {
        FoundVat foundVat = new FoundVat(
            domainName,
            siteVisit.getMatchingURL().toString(),
            siteVisit.getVat(),
            siteVisit.getVisitedURLs().size(),
            siteVisit.getTotalDuration()
        );
        foundVatRepository.save(foundVat);
      }
      crawled.incrementAndGet();
    } catch (MalformedURLException e) {
      logger.info("MalformedURLException for {}", preUrl);
    } catch (Exception e) {
      logger.info("Failure for {}", preUrl);
    }
  }

  @Test
  @Commit
  public void bigCrawlExecutor() throws InterruptedException {
    CustomizableThreadFactory tf = new CustomizableThreadFactory();
    tf.setDaemon(false);
    tf.setThreadNamePrefix("vat-crawler");
    tf.setThreadGroupName("crawler");
    ExecutorService executorService = Executors.newFixedThreadPool(20, tf);
    List<Certificate> certificates = certificateDao.findAllByVATNotNull();

    foundVatRepository.deleteAll();

    /*
    TODO: sometimes Seems to hang for a long time at

    "vat-crawler12" #203 prio=5 os_prio=31 cpu=4138.30ms elapsed=373.16s tid=0x00007ffb975a7800 nid=0xab03 in Object.wait()  [0x0000700007735000]
   java.lang.Thread.State: WAITING (on object monitor)
	at java.lang.Object.wait(java.base@13.0.1/Native Method)
	- waiting on <no object reference available>
	at java.lang.Object.wait(java.base@13.0.1/Object.java:326)
	at okhttp3.internal.http2.Http2Stream.waitForIo$okhttp(Http2Stream.kt:715)
	at okhttp3.internal.http2.Http2Stream.takeHeaders(Http2Stream.kt:140)
	- locked <0x0000000756ddd2c0> (a okhttp3.internal.http2.Http2Stream)
	at okhttp3.internal.http2.Http2ExchangeCodec.readResponseHeaders(Http2ExchangeCodec.kt:96)
	at okhttp3.internal.connection.Exchange.readResponseHeaders(Exchange.kt:106)
	at okhttp3.internal.http.CallServerInterceptor.intercept(CallServerInterceptor.kt:79)
	at okhttp3.internal.http.RealInterceptorChain.proceed(RealInterceptorChain.kt:109)
	at okhttp3.internal.connection.ConnectInterceptor.intercept(ConnectInterceptor.kt:34)
	at okhttp3.internal.http.RealInterceptorChain.proceed(RealInterceptorChain.kt:109)
	at okhttp3.internal.cache.CacheInterceptor.intercept(CacheInterceptor.kt:95)
	at okhttp3.internal.http.RealInterceptorChain.proceed(RealInterceptorChain.kt:109)
	at okhttp3.internal.http.BridgeInterceptor.intercept(BridgeInterceptor.kt:83)
	at okhttp3.internal.http.RealInterceptorChain.proceed(RealInterceptorChain.kt:109)
	at okhttp3.internal.http.RetryAndFollowUpInterceptor.intercept(RetryAndFollowUpInterceptor.kt:76)
	at okhttp3.internal.http.RealInterceptorChain.proceed(RealInterceptorChain.kt:109)
	at okhttp3.internal.connection.RealCall.getResponseWithInterceptorChain$okhttp(RealCall.kt:201)
	at okhttp3.internal.connection.RealCall.execute(RealCall.kt:154)
	at be.unamur.ct.scrap.service.PageFetcher.fetch(PageFetcher.java:92)

     */

    logger.info("certificates = {}", certificates.size());
    for (Certificate certificate : certificates) {
      String subject = certificate.getSubject();
      executorService.submit(() -> crawl(subject));
    }
    logger.info("All names submitted, waiting until done");
    executorService.shutdown();
    logger.info("executorService.isTerminated() = {}", executorService.isTerminated());
    while (!executorService.isTerminated()) {
     logger.info("waiting until done...");
      executorService.awaitTermination(20, TimeUnit.SECONDS);
    }
    logger.info("executorService.isTerminated() = {}", executorService.isTerminated());
    logger.info("crawled = {}", crawled);
  }

  @Test
  @Commit
  public void bigCrawl()  {
    foundVatRepository.deleteAll();
    List<Certificate> certificates = certificateDao.findAllByVATNotNull();
    logger.info("certificates = {}", certificates.size());
    int done = 0;
    for (Certificate certificate : certificates) {
      String subject = certificate.getSubject();
      crawl(subject);
      done++;
      logger.info("done = {}", done);
      if (done > MAX_CRAWLS) {
        break;
      }
    }
  }

  public String getSecondLevelDomainName(String subject) {
    String[] labels = subject.split("\\.");
    int labelCount = labels.length;
    if (labelCount <= 2) {
      return subject;
    }
    return labels[labelCount - 2] + "." + labels[labelCount - 1];
  }

}
