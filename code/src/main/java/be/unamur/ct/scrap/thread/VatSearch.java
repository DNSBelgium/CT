package be.unamur.ct.scrap.thread;

import be.unamur.ct.decode.model.Certificate;
import be.unamur.ct.scrap.service.VatScrapingService;


/**
 * Runnable to start the scraping of a single website
 */
public class VatSearch implements Runnable {

    private final Certificate cert;
    private final VatScrapingService vatScrapingService;

    /**
     * Constructor
     *
     * @author Jules Dejaeghere
     * @param cert        Certificate of the website to start scrapping for
     * @param vatScrapingService Reference to the VATScrapper to use
     * @see Certificate
     */
    public VatSearch(Certificate cert, VatScrapingService vatScrapingService) {
        this.cert = cert;
        this.vatScrapingService = vatScrapingService;
    }


    /**
     * Starts the VAT scrapping for the certificate saved in the variables of the instance
     *
     * @author Jules Dejaeghere
     */
    @Override
    public void run() {
        vatScrapingService.scrape(cert);
    }

}
