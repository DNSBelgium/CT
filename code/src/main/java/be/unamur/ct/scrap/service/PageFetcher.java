package be.unamur.ct.scrap.service;

import be.unamur.ct.scrap.model.PageVisit;
import okhttp3.*;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;
import java.net.URL;
import java.time.*;
import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

@Service
public class PageFetcher {

  private final OkHttpClient client;
  private final Cache cache;
  private final PageFetcherConfig config;

  private static final Logger logger = getLogger(PageFetcher.class);

  public PageFetcher(PageFetcherConfig config) {
    logger.info("config = {}", config);
    this.config = config;

    // TODO: create DNS implementation that only retrieves IPv4 addresses ??
    //  see https://square.github.io/okhttp/4.x/okhttp/okhttp3/-dns/

    cache = new Cache(config.getCacheDirectory(), config.getCacheSize().toBytes());
    logger.info("cache = {}", cache.directory());
    logger.info("cache.maxSize = {}", cache.maxSize());

    this.client = new OkHttpClient.Builder()
        .connectTimeout(config.getConnectTimeOut())
        .writeTimeout(config.getWriteTimeOut())
        .readTimeout(config.getReadTimeOut())
        .callTimeout(config.getCallTimeOut())
        .cache(cache)
        .dns(Dns.SYSTEM)
        .followRedirects(true)
        .followSslRedirects(true)
        .retryOnConnectionFailure(true)
        .minWebSocketMessageToCompress(1024)
        .connectionSpecs(List.of(ConnectionSpec.COMPATIBLE_TLS, ConnectionSpec.CLEARTEXT))
        .build();
  }

  @SuppressWarnings("ConstantConditions")
  @PreDestroy
  public void close() {
    logger.info("Closing the PageFetcher");

    logger.info("cache.hitCount = {}", cache.hitCount());
    logger.info("cache.networkCount = {}", cache.networkCount());
    logger.info("cache.requestCount = {}", cache.requestCount());

    try {
      client.dispatcher().executorService().shutdown();
      client.connectionPool().evictAll();
      client.cache().close();
    } catch (IOException e) {
      logger.warn("Error during close", e);
    }
  }

  public PageVisit fetch(URL url) throws IOException {
    return fetch(url.toString());
  }

  @SuppressWarnings("ConstantConditions")
  public PageVisit fetch(String url) throws IOException {
    String cacheControl = "max-stale=" + config.getCacheMaxStale().toSeconds();
    Request request = new Request.Builder()
        .url(url)
        .header("Cache-Control", cacheControl)
        .build();

    try (Response response = client.newCall(request).execute()) {
      if (logger.isDebugEnabled()) {
        debug(response);
      }
      long millis = response.receivedResponseAtMillis() - response.sentRequestAtMillis();
      logger.debug("millis = {}", millis);

      String body = response.body().string();

      Instant sentRequest = Instant.ofEpochMilli(response.sentRequestAtMillis());
      Instant receivedResponse = Instant.ofEpochMilli(response.receivedResponseAtMillis());

      Duration duration = Duration.between(sentRequest, receivedResponse);

      String tlsVersion = "";
      String cipherSuite = "";

      if (response.handshake() != null) {
        tlsVersion = response.handshake().tlsVersion().javaName();
        cipherSuite = response.handshake().cipherSuite().javaName();
      }

      PageVisit visit = new PageVisit(
          request.url().url(),
          receivedResponse,
          response.code(),
          tlsVersion,
          cipherSuite,
          response.protocol().toString(),
          body,
          duration);

      logger.debug("visit = {}", visit);

      return visit;

    } catch (SSLHandshakeException e) {
      logger.info("Failed to fetch {} because of {}", url, e.getMessage());
      return PageVisit.failed(request.url().url(), Instant.now());
    }

  }

  public void debug(Response response) {
    logger.debug("response = {}", response);
    logger.debug("response.code = {}", response.code());
    logger.debug("response.isSuccessful = {}", response.isSuccessful());
    logger.debug("response.message = {}", response.message());
    logger.debug("response.priorResponse = {}", response.priorResponse());
    logger.debug("response.cacheResponse = {}", response.cacheResponse());
    logger.debug("response.networkResponse = {}", response.networkResponse());
    logger.debug("response.handshake = {}", response.handshake());
    logger.debug("response.protocol = {}", response.protocol());
    logger.debug("response.sentRequestAtMillis = {}", response.sentRequestAtMillis());
    logger.debug("response.receivedResponseAtMillis = {}", response.receivedResponseAtMillis());
    //noinspection ConstantConditions
    logger.debug("response.body.contentLength = {}", response.body() == null ? 0 : response.body().contentLength());
  }


}
