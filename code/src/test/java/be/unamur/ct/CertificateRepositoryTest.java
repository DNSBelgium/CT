package be.unamur.ct;


import be.unamur.ct.data.dao.CertificateDao;
import be.unamur.ct.decode.model.Certificate;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@DataJpaTest
public class CertificateRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CertificateDao certificateDao;


    @Before
    public void setupDatabase(){
        Certificate cert = new Certificate();
        cert.setSubject("www.test.com");
        cert.setVatSearched(true);
        cert.setVAT("BE0123456789");
        cert.setIssuer("AddTrust External CA Root");
        cert.setNotAfter(new Date());
        cert.setNotBefore(new Date());
        entityManager.persist(cert);

        Certificate cert2 = new Certificate();
        cert2.setSubject("www.test.org");
        cert2.setVatSearched(true);
        entityManager.persist(cert2);

        Certificate cert3 = new Certificate();
        cert3.setSubject("www.example.com");
        cert3.setVatSearched(true);
        cert3.setVAT("BE0987654321");
        entityManager.persist(cert3);

        Certificate cert4 = new Certificate();
        cert4.setSubject("www.example.org");
        entityManager.persist(cert4);


        entityManager.flush();
    }

    @Test
    public void testFindAllOrderedById(){
        List<Certificate> found = certificateDao.findAllByOrderByIdAsc(PageRequest.of(0, Integer.MAX_VALUE));


        assertTrue(found.size() == 4);
        assertTrue(found.get(0).getId() < found.get(1).getId());

        Certificate firstFound = found.get(0);

        assertThat(firstFound.getSubject()).isEqualTo("www.test.com");
        assertTrue(firstFound.isVatSearched());
    }


    @Test
    public void testFindAllVATNotNullOrderedById(){
        List<Certificate> found = certificateDao.findAllByVATNotNullOrderByIdAsc(PageRequest.of(0, Integer.MAX_VALUE));


        assertTrue(found.size() == 2);

        for(Certificate c : found){
            assertNotNull(c.getVAT());
            assertTrue(c.isVatSearched());
        }

        assertTrue(found.get(0).getId() < found.get(1).getId());

        assertThat(found.get(0).getVAT()).isEqualTo("BE0123456789");
        assertThat(found.get(1).getVAT()).isEqualTo("BE0987654321");
    }


    @Test
    public void testCountByVATMethods(){
        int vatNullAndNotSearched = certificateDao.countByVATIsNullAndVatSearched(false);
        int vatNullAndSearched = certificateDao.countByVATIsNullAndVatSearched(true);

        int vatNotNullAndSearched = certificateDao.countByVATIsNotNullAndVatSearched(true);
        int vatNotNullAndNotSearched = certificateDao.countByVATIsNotNullAndVatSearched(false);


        assertTrue(vatNullAndNotSearched == 1);
        assertTrue(vatNullAndSearched == 1);

        assertTrue(vatNotNullAndNotSearched == 0);
        assertTrue(vatNotNullAndSearched == 2);

    }


    @Test
    public void testFindByVatSearched(){
        List<Certificate> vatSearched = certificateDao.findByVatSearched(true);
        List<Certificate> vatNotSearched = certificateDao.findByVatSearched(false);


        assertTrue(vatNotSearched.size() == 1);
        assertTrue(vatSearched.size() == 3);

        for(Certificate c : vatSearched){
            assertFalse(vatNotSearched.contains(c));
        }
    }
}
