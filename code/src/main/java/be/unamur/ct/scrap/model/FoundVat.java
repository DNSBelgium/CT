package be.unamur.ct.scrap.model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.validation.constraints.NotNull;
import java.time.Duration;
import java.util.StringJoiner;

@Entity
public class FoundVat {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private long id;

  @NotNull
  private String domainName;

  private String url;

  private String vat;

  private int visitedURLs;

  private double totalDurationInSeconds;

  public FoundVat() {
  }

  public FoundVat(String domainName, String url, String vat, int visitedURLs, Duration totalDuration) {
    this.domainName = domainName;
    this.url = url;
    this.vat = vat;
    this.visitedURLs = visitedURLs;
    this.totalDurationInSeconds = totalDuration.toMillis() / 1000.0;
  }

  public long getId() {
    return id;
  }

  public String getUrl() {
    return url;
  }

  public String getVat() {
    return vat;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", FoundVat.class.getSimpleName() + "[", "]")
        .add("id=" + id)
        .add("domainName='" + domainName + "'")
        .add("url='" + url + "'")
        .add("vat='" + vat + "'")
        .add("visitedURLs='" + visitedURLs + "'")
        .add("totalDurationInSeconds='" + totalDurationInSeconds + "'")
        .toString();
  }

}
