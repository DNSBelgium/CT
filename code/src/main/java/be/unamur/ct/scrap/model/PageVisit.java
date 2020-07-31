package be.unamur.ct.scrap.model;

import lombok.Data;
import org.jsoup.nodes.Document;

import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.StringJoiner;

@Data
public class PageVisit {

  private final URL url;
  private final Instant visited;
  private final int statusCode;
  private final String tlsVersion;
  private final String cipherSuite;

  // the protocol used to retrieve the page content (eg. "spdy/3.1" or "")
  // https://square.github.io/okhttp/4.x/okhttp/okhttp3/-protocol/
  private final String protocol;

  private final String body;
  private String bodyText;

  private boolean vatFound;
  private String vat;

  private Document document;

  private final Duration visitDuration;

  public static PageVisit failed(URL url, Instant visited) {
    return new PageVisit(url, visited, 0, "", "", "", "", Duration.ZERO);
  }

  public void setDocument(Document document) {
    this.document = document;
  }

  public boolean hasContent() {
    return body != null && body.length() > 0;
  }

  /*
    Data to return after a VAT search
    * number of links followed
    * timestamp start
    * timestamp done
    * For each visited URL :
    *  the URL
    *  the http status code ?
    *  the content (before or after JSoup parsing?)
    *  the length of the content
    *  time to retrieve content
     */


  @Override
  public String toString() {
    return new StringJoiner(", ", PageVisit.class.getSimpleName() + "[", "]")
        .add("url=" + url)
        .add("visited=" + visited)
        .add("visitDuration=" + visitDuration)
        .add("statusCode=" + statusCode)
        .add("tlsVersion=" + tlsVersion)
        .add("cipherSuite=" + cipherSuite)
        .add("body.length()=" + body.length())
        .add("protocol=" + protocol)
        .add("tlsVersion=" + tlsVersion)
        .toString();
  }

}
