package be.unamur.ct.scrap.service;

import be.unamur.ct.data.dao.CertificateDao;
import be.unamur.ct.decode.model.Certificate;
import be.unamur.ct.scrap.thread.VATScrapperThread;
import be.unamur.ct.thread.ThreadPool;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Service class providing methods to scrap a website searching for a VAT number
 * VAT number are stored in the database with the certificate they are related to
 */
@Service
public class VATScrapper {

    @Autowired
    private CertificateDao certificateDao;
    @Autowired
    private ThreadPool threadPool;

    private Logger logger = LoggerFactory.getLogger(VATScrapper.class);
    private int timeout = 5000;
    private int depth = 5;
    private String patternVAT = "(?i)((BE)?0([. -])?[0-9]{3}([. -])?[0-9]{3}([. -])?[0-9]{3})";


    public VATScrapper() {}


    /**
     * Scraps a website based on the certificate searching for a VAT number.
     * If a VAT number is found, the certificate is updated in the database.
     *
     * @author Jules Dejaeghere
     * @param certificate Certificate of the website to scrap
     */
    public void scrap(Certificate certificate) {
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

        try {
            String VAT = searchPage(url, depth, new HashSet<URL>());
            if (VAT != null) {
                certificate.setVAT(VAT);
                logger.info("Cert of " + certificate.getSubject() + " saved with VAT " + VAT);
            } else {
                logger.info("VAT not found for " + certificate.getSubject());
            }
            certificate.setVatSearched(true);
            certificateDao.save(certificate);
        } catch (InterruptedException e) {
        }
    }


    /**
     * Searches the page at the give url for a VAT number.
     * If no VAT number is found, recursively calls itself for each link of the same domain on the page.
     *
     * @author Jules Dejaeghere
     * @param url     URL to search
     * @param depth   Maximum depth allowed for future searches.  If it reaches 0, search is aborted.
     * @param visited HashSet of URL already visited in previous calls for this website
     * @return Nullable String containing the normalized VAT if found on the page or in one of the recursive calls
     * @throws InterruptedException if the thread is interrupted
     */
    public String searchPage(URL url, int depth, HashSet<URL> visited) throws InterruptedException {

        /* TODO:
         *  Find a better way to stop the thread
         *  The Thread.interrupt() method doesn't seem to work
         */
//        if (threadPool.getVATScrapperExecutor().isShutdown()) {
//            throw new InterruptedException();
//        }

        //  Check if it is needed to visit URL
        if (depth < 0 || visited.contains(url)) {
            return null;
        } else {
            visited.add(url);
        }

        // Retrieve web page
        Document document;

        try {
            document = Jsoup.parse(url, timeout);
        } catch (Exception e) {
            // TODO debug log ?
            return null;
        }

        // Search for VAT number
        String text;

        try {
            text = document.body().text();
        } catch (NullPointerException e) {
            return null;
        }

        // TODO: compile regex only once
        Pattern r = Pattern.compile(patternVAT);
        Matcher m = r.matcher(text);

        if (m.find()) {
            String VAT = m.group(1);
            VAT = normalizeVAT(VAT);

            if (isValidVAT(VAT)) {
                return VAT;
            }
        }

        // If not VAT number on page, search links on page and follow them
        // Create a list of links to follow.  Links in footer appear first
        ArrayList<URL> linksOnPage = new ArrayList<URL>();

        // Search links in footer
        Elements footer = document.getElementsByTag("footer");
        for (Element f : footer) {
            Elements links = f.getElementsByTag("a");

            for (Element l : links) {
                try {
                    linksOnPage.add(new URL(l.absUrl("href")));
                } catch (MalformedURLException e) {
                }
            }
        }

        // Search links in the whole page
        Elements links = document.body().getElementsByTag("a");
        for (Element l : links) {
            try {
                linksOnPage.add(new URL(l.absUrl("href")));
            } catch (MalformedURLException e) {
            }
        }

        // If link points to same sub-domain, follow it
        String hostBase = new String();
        String hostLink = new String();

        // TODO: try to understand this code.
        //  Why not give host as a parameter instead of extracting it from the url again and again
        // TODO: make method to extract 'host' (actually domain name ?)  from URL
        //  goal is to find inner links. Check if JSoup has method for this ? How done in fake shop detector ?

        try {
            String host = url.toURI().getHost();
            hostBase = host.startsWith("www.") ? host.substring(4) : host;
        } catch (URISyntaxException e) {
        }


        for (URL link : linksOnPage) {
            boolean sameDomain = false;
            if (link != null) {
                try {
                    String host = link.toURI().getHost();
                    hostLink = host.startsWith("www.") ? host.substring(4) : host;
                } catch (URISyntaxException | NullPointerException e) {
                }

                sameDomain = hostBase.equals(hostLink);
            }

            if (sameDomain) {
                String VAT = searchPage(link, depth - 1, visited);
                if (VAT != null) {
                    return VAT;
                }
            }
        }

        return null;
    }


    /**
     * Normalizes a raw VAT number found on a web page to fit the format BExxxxxxxxxx where x is a digit
     *
     * @author Jules Dejaeghere
     * @param VAT Raw VAT number found on a web page
     * @return Nullable (if input is null) String with the normalized VAT number
     */
    public String normalizeVAT(String VAT) {

        if (VAT == null) {
            return null;
        }

        VAT = VAT.toUpperCase();

        VAT = VAT.replace(".", "")
                .replace("-", "")
                .replace(" ", "");

        VAT = (VAT.startsWith("BE") ? VAT : "BE" + VAT);

        return VAT;
    }


    /**
     * Determines if a normalized VAT number is valid.
     * A valid VAT number meet the following criteria:
     *  - Let BE0xxx xxx xyy a VAT number
     *  - The VAT number if valid if 97-(xxxxxxx mod 97) == yy
     *
     * @author Jules Dejaeghere
     * @param VAT   Normalized VAT number to check
     * @return      true if the VAT is valid, false otherwise
     */
    public boolean isValidVAT(String VAT) {

        VAT = VAT.substring(2);

        int head = Integer.parseInt(VAT.substring(0, VAT.length() - 2));
        int tail = Integer.parseInt(VAT.substring(8));


        return (97 - (head % 97)) == tail;
    }


    /**
     * Resume VAT scrapping after a restart of the application.
     * Find all certificate not yet searched for VAT number and
     * creates a scrapping thread for each certificate matching the query
     *
     * @author Jules Dejaeghere
     */
    public void resumeVatScrapping() {

        List<Certificate> cert = certificateDao.findByVatSearched(false);
        logger.info("Restarting " + cert.size() + " searches");

        for (Certificate c : cert) {
            threadPool.getVATScrapperExecutor().execute(new VATScrapperThread(c, this));
        }

        logger.info("Restarted " + cert.size() + " searches");
    }
}