package be.unamur.ct.scrap.service;


import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.slf4j.LoggerFactory.getLogger;

class VatFinderTest {

  private final VatFinder vatFinder = new VatFinder();
  private static final Logger logger = getLogger(VatFinderTest.class);

  @Test
  public void testNormalizeVAT(){
    String[] raw = {"BE0123 456 346", "BE0123-456-346", "BE0123.456.346",
        "0123 456 346", "0123456346", "0123.456.346"};

    String normalized;
    for(String vat : raw){
      normalized = vatFinder.normalizeVAT(vat);
      assertThat(normalized).isEqualTo("BE0123456346");
    }

  }


  @Test
  public void testIsValidVAT(){
    String[] valid = {"BE0666679317", "BE0457741515", "BE0843370953"};
    String[] invalid = {"BE0666679300", "BE0457741542", "BE0843370973"};

    for(String vat : valid){
      assertTrue(vatFinder.isValidVAT(vat));
    }

    for(String vat : invalid){
      assertFalse(vatFinder.isValidVAT(vat));
    }
  }

  @Test
  public void findMultipleVatValues() {
    String input = "abc BE0666679317 cdef BE0457741515 xyz BE0843370953";

    List<String> list = vatFinder.findVatValues(input);
    logger.info("list = {}", list);

  }

}