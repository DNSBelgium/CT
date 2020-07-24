package be.unamur.ct;

import be.unamur.ct.data.dao.CertificateDao;
import be.unamur.ct.data.service.CertificateService;
import org.assertj.core.util.Lists;
import org.javatuples.Pair;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringRunner;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
public class CertificateServiceTest {

    @TestConfiguration
    static class CertificateServiceTestContextConfiguration {

        @Bean
        public CertificateService certificateService() {
            return new CertificateService();
        }
    }


    @MockBean
    private CertificateDao certificateDao;

    @SuppressWarnings("SpringJavaAutowiredMembersInspection")
    @Autowired
    private CertificateService certificateService;

    @Before
    public void setupMock(){
        Mockito.when(certificateDao.countByVATIsNotNullAndVatSearched(true))
                .thenReturn(2);
        Mockito.when(certificateDao.countByVATIsNullAndVatSearched(true))
                .thenReturn(1);
        Mockito.when(certificateDao.countByVATIsNullAndVatSearched(false))
                .thenReturn(1);

        List<Object[]> issuer = new ArrayList<>();
        issuer.add(new Object[]{"Bob", BigInteger.valueOf(1)});
        issuer.add(new Object[]{"Alice", BigInteger.valueOf(3)});

        Mockito.when(certificateDao.distinctIssuer())
                .thenReturn(issuer);

        List<Object[]> algo = new ArrayList<>();
        algo.add(new Object[]{"Alg1", BigInteger.valueOf(3)});
        algo.add(new Object[]{"Alg2", BigInteger.valueOf(1)});

        Mockito.when(certificateDao.distinctAlgorithm())
                .thenReturn(algo);
    }


    @Test
    public void testVatGraphData(){
        ArrayList<Integer> result = certificateService.vatGraphData();
        assertThat(result).isEqualTo(Lists.newArrayList(2,1,1));
    }


    @Test
    public void testIssuerGraphData(){
        Pair<ArrayList<BigInteger>, ArrayList<String>> result = certificateService.issuerGraphData();
        ArrayList<BigInteger> num = bigIntegerList(3,1);
        ArrayList<String> labels = Lists.newArrayList("Alice", "Bob");
        Pair<ArrayList<BigInteger>, ArrayList<String>> expected = new Pair<>(num, labels);
        checkList(result, expected);
    }


    @Test
    public void testAlgorithmGraphData(){
        Pair<ArrayList<BigInteger>, ArrayList<String>> result = certificateService.algorithmGraphData();
        ArrayList<BigInteger> num = Lists.newArrayList(BigInteger.valueOf(3), BigInteger.valueOf(1));
        Pair<ArrayList<BigInteger>, ArrayList<String>> expected = new Pair<>(num, Lists.newArrayList("Alg1", "Alg2"));
        checkList(result, expected);

    }

    private void checkList(Pair<ArrayList<BigInteger>, ArrayList<String>> result,
                           Pair<ArrayList<BigInteger>, ArrayList<String>> expected) {

        assertThat(result.getValue0()).isEqualTo(expected.getValue0());
        assertThat(result.getValue1()).isEqualTo(expected.getValue1());
    }

    private ArrayList<BigInteger> bigIntegerList(long ... values) {
        return Arrays.stream(values).mapToObj(BigInteger::valueOf).collect(Collectors.toCollection(ArrayList::new));
    }

    @Test
    public void testList() {
        List<BigInteger> x = bigIntegerList(1, 5, 8);
        System.out.println("x = " + x);
    }

}
