package be.unamur.ct.scrap.service;

import be.unamur.ct.data.dao.CertificateDao;
import be.unamur.ct.decode.model.Certificate;
import be.unamur.ct.scrap.model.SiteVisit;
import be.unamur.ct.scrap.thread.VatSearch;
import be.unamur.ct.thread.ThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;


/**
 * Service class providing methods to scrape a website searching for a VAT number
 * VAT number are stored in the database with the certificate they are related to
 */
@Service
public class VatScrapingService {

    private final CertificateDao certificateDao;
    private final VatScraper vatScraper;

    int maxVisitsPerSite = 30;

    private final static Logger logger = LoggerFactory.getLogger(VatScrapingService.class);

    @Autowired
    public VatScrapingService(CertificateDao certificateDao, VatScraper vatScraper) {
        this.certificateDao = certificateDao;
        this.vatScraper = vatScraper;
    }

    /**
     * Scrapes a website based on the certificate searching for a VAT number.
     * If a VAT number is found, the certificate is updated in the database.
     *
     * @author Jules Dejaeghere
     * @param certificate Certificate of the website to scrap
     */
    public void scrape(Certificate certificate) {
        String subject = certificate.getSubject();
        String preUrl = "http://" + (subject.startsWith("*.") ? "www." + subject.substring(2) : subject);

        URL url;
        try {
            url = new URL(preUrl);
        } catch (MalformedURLException e) {
            logger.warn("Cannot make an URL from " + subject);
            return;
        }

        logger.info("Scrapping from base URL: " + url.toString());

        SiteVisit siteVisit = vatScraper.visit(url, maxVisitsPerSite);
        if (siteVisit.isVatFound()) {
            String vat = siteVisit.getVat();
            certificate.setVAT(vat);
            logger.info("Cert of " + certificate.getSubject() + " saved with VAT " + vat);
        } else {
            logger.info("No VAT found for " + certificate.getSubject());
        }
        certificate.setVatSearched(true);
        certificateDao.save(certificate);
    }


    /**
     * Resume VAT scraping after a restart of the application.
     * Find all certificates for which we have not yet done a VAT search and
     * submit a task for each certificate matching the query
     *
     * @author Jules Dejaeghere
     */
    public void resumeVatScrapping() {
        List<Certificate> cert = certificateDao.findByVatSearched(false);
        logger.info("Restarting " + cert.size() + " searches");
        for (Certificate c : cert) {
            ThreadPool.getVATScrapperExecutor().execute(new VatSearch(c, this));
        }
        logger.info("Restarted " + cert.size() + " searches");
    }
}