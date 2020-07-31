package be.unamur.ct.scrap.service;

import be.unamur.ct.scrap.model.PageVisit;
import be.unamur.ct.scrap.model.SiteVisit;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;

@Service
public class VatScraper {

  private final PageFetcher pageFetcher;
  private final VatFinder vatFinder;

  private static final Logger logger = getLogger(VatScraper.class);

  @Autowired
  public VatScraper(PageFetcher pageFetcher, VatFinder vatFinder) {
    this.pageFetcher = pageFetcher;
    this.vatFinder = vatFinder;
  }

  String[] keywords = {
      "about", "contact", "over", "address", "locat", "adres", "policy", "condition", "bezoek-ons", "ons", "legal", "privacy", "disclaimer",
      "voorwaarden", "btw", "vat", "nous"};

  /*
  l’adresse du siège social est située avenue de la Couronne, 321 à 1050 Bruxelles, Belgique ;
  le numéro d’entreprise est le 0416.971.722 ;
  le numéro de compte bancaire est le BE07 0682 0302 4966
   */

  // TODO: even better would be to assign a weight to these keywords (eg. first check "about-us" and then "legal")

  public int sortLinks(String link1, String link2) {

    boolean link1HasPriority = StringUtils.containsAny(link1, keywords);
    boolean link2HasPriority = StringUtils.containsAny(link2, keywords);

    if (link1HasPriority && !link2HasPriority) {
      return -1;
    }
    if (link2HasPriority && !link1HasPriority) {
      return 1;
    }
    // either both links have priority or none has priority => use
    return 0;
  }

  public SiteVisit visit(URL url, int maxVisits) {
    SiteVisit siteVisit = new SiteVisit(url);
    logger.debug("Analyze landing page {}", url);

    // this will either find a VAT or add inner links to the queue
    visit(siteVisit, url);

    if (!siteVisit.isVatFound()) {
      logger.debug("No VAT found on landing page => crawling inner links");
      while (true) {
        URL next = siteVisit.popFromQueue();
        if (next == null) {
          logger.debug("No more links to follow for {} => done", url);
          break;
        }
        if (siteVisit.getPageVisitList().size() >= maxVisits) {
          logger.debug("reached max number of page visits per site for {} => done", url);
          break;
        }
        if (siteVisit.isVatFound()) {
          logger.debug("Found VAT {} for {} after {} visits => done", siteVisit.getVat(), url, siteVisit.getPageVisitList().size());
          break;
        }
        logger.debug("Visiting {} for {}", next, url);
        visit(siteVisit, next);
      }
    }
    logger.debug("Finished scraping for {} => vatFound = {}", url, siteVisit.isVatFound());
    return siteVisit;
  }

  private void visit(SiteVisit siteVisit, URL url) {
    if (siteVisit.alreadyVisited(url)) {
      logger.debug("we already visited {}", url);
      return;
    }
    PageVisit pageVisit = fetchAndParse(url);
    if (pageVisit == null) {
      logger.debug("No content found on {}", url);
      return;
    }
    if (pageVisit.getDocument().body() == null) {
      logger.debug("Document on {} has no body", url);
      return;
    }
    findVAT(pageVisit);
    siteVisit.add(pageVisit);

    if (!pageVisit.isVatFound()) {
      List<String> innerLinks = findInnerLinks(url, pageVisit.getDocument());
      innerLinks.sort(this::sortLinks);
      for (String innerLink : innerLinks) {
        try {
          URL innerLinkURL = new URL(innerLink);
          siteVisit.pushToQueue(innerLinkURL);
        } catch (MalformedURLException e) {
          logger.warn("Failed to create URL from {} because of {}", innerLink, e.getMessage());
        }
      }
    }
  }

  public void findVAT(PageVisit pageVisit) {
      Element body = pageVisit.getDocument().body();
      if (body == null) {
        logger.debug("Document on {} has no body", pageVisit.getUrl());
        return;
      }
      String text = pageVisit.getDocument().body().text();
      logger.debug("url={} text.length={} ", pageVisit.getUrl(), text.length());

      List<String> validVatValues = vatFinder.findValidVatValues(text);
      if (validVatValues.size() > 0) {
        logger.info("{} => Found {} valid VAT values {}", pageVisit.getUrl(), validVatValues.size(), validVatValues);

        if (validVatValues.size() > 3) {
          // sometimes binary files or text files have many matches
          logger.warn("Found more than 3 valid VAT values. Probably not reliable => not saving VAT value");
        } else {
          // just pick the first one for now
          pageVisit.setVat(validVatValues.get(0));
          pageVisit.setVatFound(true);
        }
      } else {
        logger.debug("probably not interesting to record VAT values with wrong checksum ??");
        logger.debug("No valid VAT values found => search for VAT-like values");
        List<String> vatLikeValues = vatFinder.findVatValues(text);
        if (vatLikeValues.size() > 0 && validVatValues.size() < 3) {
          // TODO: save VAT-like values this to a separate field
          logger.info("{} => Found VAT-like values {}", pageVisit.getUrl(), vatLikeValues);
          // pageVisit.setVat(vatLikeValues.get(0));
          pageVisit.setVatFound(false);
        }
      }
  }

  public PageVisit fetchAndParse(URL url) {
    try {
      PageVisit pageVisit = pageFetcher.fetch(url);
      if (pageVisit.hasContent()) {
        String body = pageVisit.getBody();
        Document document = Jsoup.parse(body, url.toString());
        pageVisit.setDocument(document);
        pageVisit.setBodyText(body);
        return pageVisit;
      }
    } catch (IOException e) {
      logger.debug("Failed to fetch {} because of {}", url, e.getMessage());
      return null;
    }
    return null;
  }

  public List<String> findInnerLinks(URL url, Document document) {
    ArrayList<String> linksOnPage = new ArrayList<>();
    Elements footer = document.getElementsByTag("footer");
    for (Element f : footer) {
      Elements links = f.getElementsByTag("a");
      for (Element link : links) {
        linksOnPage.add(link.absUrl("href"));
      }
    }
    if (document.body() == null) {
      return Collections.emptyList();
    }
    Elements links = document.body().getElementsByTag("a");
    for (Element link : links) {
      linksOnPage.add(link.absUrl("href"));
    }
    String domainName = getSecondLevelDomainName(url);
    return linksOnPage.stream()
        .filter(s -> getSecondLevelDomainName(s).equals(domainName))
        .filter(s -> !s.endsWith("pdf"))
        .filter(s -> !s.endsWith("PDF"))
        .collect(Collectors.toList());
  }



  /**
   * return the second-level domain name based on the host og given URL (last two labels of the domain name
   *
   * Will return last two bytes when host is an IPv4 address !
   * (Does not really matter since we use this method to find inner links and always compare with a real domain name)
   *
   * Does not take into public suffix list. So getSecondLevelDomainName("bbc.co.uk") => "co.uk"
   *
   * @param url the url to start from
   * @return the second-level domain name based on the host of given URL
   */
  public String getSecondLevelDomainName(String url) {
    try {
      return getSecondLevelDomainName(new URL(url));
    } catch (MalformedURLException e) {
      return "";
    }
  }

  public String getSecondLevelDomainName(URL url) {
    String host = url.getHost();
    String[] labels = host.split("\\.");
    int labelCount = labels.length;
    if (labelCount <= 2) {
      return host;
    }
    return labels[labelCount - 2] + "." + labels[labelCount - 1];
  }


}
