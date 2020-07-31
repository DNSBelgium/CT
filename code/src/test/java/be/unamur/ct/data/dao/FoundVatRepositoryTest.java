package be.unamur.ct.data.dao;

import be.unamur.ct.scrap.model.FoundVat;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.Commit;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.slf4j.LoggerFactory.getLogger;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
public class FoundVatRepositoryTest {

  @Autowired
  private FoundVatRepository foundVatRepository;


  private static final Logger logger = getLogger(FoundVatRepositoryTest.class);

  @Test
  @Commit
  public void insert() {
    FoundVat vat = new FoundVat("abc.be", "https://abc.be/contact-us", "BE0123456789", 5,
        Duration.ofMillis(5));
    foundVatRepository.save(vat);
    logger.info("vat = {}", vat.getId());
    List<FoundVat> all = foundVatRepository.findAll();
    logger.info("all = {}", all);
    assertThat(all.size()).isGreaterThan(0);
  }

}