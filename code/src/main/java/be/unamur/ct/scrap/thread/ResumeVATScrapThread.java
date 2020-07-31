package be.unamur.ct.scrap.thread;

import be.unamur.ct.scrap.service.VatScrapingService;


/**
 * Thread class to resume VAT scrapping when the application is restarted
 */
public class ResumeVATScrapThread extends Thread {

    private final VatScrapingService vatScrapingService;

    /**
     * Constructor
     *
     * @author Jules Dejaeghere
     * @param vatScrapingService Reference to the VATScrapper to use
     */
    public ResumeVATScrapThread(VatScrapingService vatScrapingService) {
        this.vatScrapingService = vatScrapingService;
    }


    /**
     * Resumes the VATScrapping
     *
     * @author Jules Dejaeghere
     */
    @Override
    public void run() {
        vatScrapingService.resumeVatScrapping();
    }
}
