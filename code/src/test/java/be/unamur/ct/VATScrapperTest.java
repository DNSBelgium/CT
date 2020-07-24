package be.unamur.ct;


import be.unamur.ct.scrap.service.VATScrapper;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.net.URL;
import java.util.HashSet;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;
import static org.slf4j.LoggerFactory.getLogger;

public class VATScrapperTest {

    private final VATScrapper vatScrapper = new VATScrapper();
    private static final Logger logger = getLogger(VATScrapperTest.class);

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(options().port(8080));

    @Before
    public void setup() throws IOException {
        //  Creating small test web server to serve web pages to scrap
        stubFor("/", "html/simple/index.html");

        for (String s : new String[] {"index", "page-1", "page-2"}) {
            stubFor("/complex/" + s, "html/complex/" + s + ".html");
        }

        for (String s : new String[] {"wrong-checksum", "wrong-format"}) {
            stubFor("/wrong/" + s, "html/wrong/" + s + ".html");
        }
    }

    public void stubFor(String url, String resourcePath) throws IOException {
        byte[] body = FileCopyUtils.copyToByteArray( new ClassPathResource(resourcePath).getInputStream());
        logger.info("Setiing up stub content for url={} => {}", url, resourcePath);
        wireMockRule.stubFor(get(urlEqualTo(url))
            .willReturn(aResponse()
                .withBody(body)
                .withHeader("Content-Type", "text/html; charset=UTF-8"))
        );
    }


    @Test
    public void testSearchPage() throws IOException, InterruptedException {

        String vat;

        // Easy case: VAT on first page
        vat = vatScrapper.searchPage(new URL("http://localhost:8080/"), 0, new HashSet<>());
        assertThat(vat).isEqualTo("BE0542703815");


        // VAT not on first page and depth too small
        vat = vatScrapper.searchPage(new URL("http://localhost:8080/complex/index"), 0, new HashSet<>());
        assertNull(vat);


        // VAT not on first page and depth large enough to find it on second page
        vat = vatScrapper.searchPage(new URL("http://localhost:8080/complex/index"), 3, new HashSet<>());
        assertThat(vat).isEqualTo("BE0542703815");

        // VAT not found because of wrong format
        vat = vatScrapper.searchPage(new URL("http://localhost:8080/wrong/wrong-format"), 0, new HashSet<>());
        assertNull(vat);

        // VAT not found because of wrong checksum
        vat = vatScrapper.searchPage(new URL("http://localhost:8080/wrong/wrong-checksum"), 0, new HashSet<>());
        assertNull(vat);
    }


    @Test
    public void testNormalizeVAT(){
        String[] raw = {"BE0123 456 346", "BE0123-456-346", "BE0123.456.346",
                        "0123 456 346", "0123456346", "0123.456.346"};

        String normalized;
        for(String vat : raw){
            normalized = vatScrapper.normalizeVAT(vat);
            assertThat(normalized).isEqualTo("BE0123456346");
        }

    }


    @Test
    public void testIsValidVAT(){
        String[] valid = {"BE0666679317", "BE0457741515", "BE0843370953"};
        String[] invalid = {"BE0666679300", "BE0457741542", "BE0843370973"};

        for(String vat : valid){
            assertTrue(vatScrapper.isValidVAT(vat));
        }

        for(String vat : invalid){
            assertFalse(vatScrapper.isValidVAT(vat));
        }
    }

}
