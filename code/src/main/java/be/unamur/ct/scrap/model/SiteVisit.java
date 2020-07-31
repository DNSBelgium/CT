package be.unamur.ct.scrap.model;

import lombok.Data;
import org.slf4j.Logger;

import java.net.URL;
import java.time.Duration;
import java.util.*;

import static org.slf4j.LoggerFactory.getLogger;

@Data
public class SiteVisit {

  // the URL where the visit started
  private final URL baseURL;

  private final List<PageVisit> pageVisitList;

  private final List<URL> visitedURLs;

  private final Set<URL> todo;
  private final Queue<URL> queue;

  private URL matchingURL;

  private String vat;
  private boolean vatFound = false;

  private static final Logger logger = getLogger(SiteVisit.class);

  public SiteVisit(URL baseURL) {
    this.baseURL = baseURL;
    this.pageVisitList = new ArrayList<>();
    this.visitedURLs = new ArrayList<>();
    this.queue = new ArrayDeque<>();
    this.todo = new HashSet<>();
  }

  public void add(PageVisit pageVisit) {
    pageVisitList.add(pageVisit);
    visitedURLs.add(pageVisit.getUrl());
    if (pageVisit.isVatFound()) {
      this.vatFound = true;
      this.vat = pageVisit.getVat();
      this.matchingURL = pageVisit.getUrl();
      logger.debug("VAT found on {} after {} page visits", matchingURL, pageVisitList.size());
    }
  }

  public Duration getTotalDuration() {
    Duration totalDuration = Duration.ZERO;
    for (PageVisit pageVisit : pageVisitList) {
      totalDuration = totalDuration.plus(pageVisit.getVisitDuration());
    }
    return totalDuration;
  }

  public boolean alreadyVisited(URL url) {
    return visitedURLs.contains(url);
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", SiteVisit.class.getSimpleName() + "[", "]")
        .add("baseURL=" + baseURL)
        .add("pageVisitList=" + pageVisitList.size())
        .toString();
  }

  public void pushToQueue(URL url) {
    if (!todo.contains(url)) {
      todo.add(url);
      queue.add(url);
      logger.debug("URL pushed to queue: {} for {}", url, this.baseURL);
      logger.debug("queue.size = {}", queue.size());
    }
  }

  public URL popFromQueue() {
    URL url = queue.poll();
    logger.debug("URL popped from queue: {}", url);
    logger.debug("queue.size = {}", queue.size());
    return url;
  }

}
