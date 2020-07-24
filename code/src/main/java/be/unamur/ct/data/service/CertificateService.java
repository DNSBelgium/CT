package be.unamur.ct.data.service;


import be.unamur.ct.data.dao.CertificateDao;
import be.unamur.ct.decode.model.Certificate;
import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * Service class providing methods to help certificate management in the application
 */
@Service
public class CertificateService {

    @Autowired
    private CertificateDao certificateDao;

    private Logger logger = LoggerFactory.getLogger(CertificateService.class);


    /**
     * Returns a list of certificate to be displayed.  Certificates returned in the page are fetched from the database.
     * Only certificates having a non-null value for the VAT field may be selected when using vatOnly = true
     *
     * @author Jules Dejaeghere
     * @param page Index of the page to fetch
     * @param size Size of the page to fetch
     * @param vatOnly  If set, returns only certificate having a VAT number
     * @return A list of certificates, as specified by pageable and vatOnly
     */
    public List<Certificate> findPaginatedCertificates(int page, int size, boolean vatOnly) {

        List<Certificate> list;

        if (certificateDao.count() < page*size) {
            list = Collections.emptyList();
        } else {
            Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
            list = vatOnly ? certificateDao.findAllByVATNotNullOrderByIdAsc(pageable) :
                             certificateDao.findAllByOrderByIdAsc(pageable);
        }

        return list;
    }


    /**
     * Returns an ArrayList of the amount of certification with a VAT number, without a VAT number but already scrapped
     * and not yet scrapped
     * This ArrayList is used to display a graph on the web page
     *
     * @author Jules Dejaeghere
     * @return An array of the number of certificates in the categories explained before
     */
    public ArrayList<Integer> vatGraphData() {
        //  Create graph data for the VAT numbers
        ArrayList<Integer> vatCount = new ArrayList<>();

        Integer exists = certificateDao.countByVATIsNotNullAndVatSearched(true);
        Integer notFound = certificateDao.countByVATIsNullAndVatSearched(true);
        Integer notSearched = certificateDao.countByVATIsNullAndVatSearched(false);

        if (!(exists == 0 && notFound == 0 && notSearched == 0)) {
            vatCount.add(exists);
            vatCount.add(notFound);
            vatCount.add(notSearched);
        }

        return vatCount;
    }


    /**
     * Returns a pair of arrays, counting the number of certificates for each issuer
     * The first array holds the number of occurrences of each issuer
     * The second array holds the names of the issuers
     * The arrays are cropped to six elements maximum.  If the arrays should have been longer, the exceeding elements
     * are grouped in an "Other" category at the end of the array
     * This Pair of ArrayList is used to display a graph on the web page
     *
     * @author Jules Dejaeghere
     * @return A pair of arrays, counting the number of certificates for each issuer
     */
    public Pair<ArrayList<BigInteger>, ArrayList<String>> issuerGraphData() {

        //  Create graph data for issuer
        List<Object[]> result = certificateDao.distinctIssuer();

        // TODO: seems a strange data structure. Why not create a Class to represent this data ?

        return createPairForGraph(result);
    }


    /**
     * Returns a pair of arrays, counting the number of certificates for each issuer
     * The first array holds the number of occurrences of each algorithm
     * The second array holds the names of the algorithms
     * The arrays are cropped to six elements maximum.  If the arrays should have been longer, the exceeding elements
     * are grouped in an "Other" category at the end of the array
     * This Pair of ArrayList is used to display a graph on the web page
     *
     * @author Jules Dejaeghere
     * @return A pair of arrays, counting the number of certificates for each algorithm
     */
    public Pair<ArrayList<BigInteger>, ArrayList<String>> algorithmGraphData() {

        //  Create graph data for issuer
        List<Object[]> result = certificateDao.distinctAlgorithm();

        return createPairForGraph(result);
    }


    /**
     * Give an list of Arrays of objects (each array of objects is supposed to contain a String and a BigInteger),
     * it returns a Pair of Arrays.
     * The first Array contains the BigIntegers and the second Array contains the Strings
     * Elements in the same array of object in the parameter will be at the same index in the Arrays outputted
     * The Array of BigInteger is sorted descending
     * The outputted arrays are six element long maximum.  If the list of Object[] is longer than six elements
     * the Object[] which have the lower value for the BigInteger are aggregated under the category "Other", a the end
     * of the array.
     *
     * TODO:  why is this strange data structure used ?  Is it just data shuffling for displaying purposes ? Ie view logic ?
     * TODO:  Seems to be a way to cap the info to be displayed
     *
     * Example
     * -------
     *
     * result = { ["A", 10], ["B", 3], ["C", 4], ["D", 9], ["E", 2], ["F", 9], ["G", 1] }
     * Will return Pair({10, 9, 9, 4, 3, 3}, {"A", "D", "F", "C", "B", "Others"})
     *
     * @author Jules Dejaeghere
     * @param result The List of Object[] to process
     * @return Pair of Arrays (BigInteger and String)
     */
    public Pair<ArrayList<BigInteger>, ArrayList<String>> createPairForGraph(List<Object[]> result) {
        ArrayList<Pair<String, BigInteger>> count = new ArrayList<>();

        for (Object[] o : result) {
            // TODO: use other data structure and avoid casting
            count.add(new Pair<>((String) o[0], (BigInteger) o[1]));
        }

        Collections.sort(count, (o1, o2) -> o2.getValue1().compareTo(o1.getValue1()));

        List<Pair<String, BigInteger>> data;

        if (count.size() > 6) {
            data = count.subList(0, 5);

            BigInteger others = BigInteger.ZERO;

            for (Pair<String, BigInteger> p : count.subList(5, count.size())) {
                others = others.add(p.getValue1());
            }

            data.add(new Pair<>("Others", others));
        } else {
            data = count;
        }

        ArrayList<BigInteger> num = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();
        for (Pair<String, BigInteger> p : data) {
            labels.add(p.getValue0());
            num.add(p.getValue1());
        }

        return new Pair<>(num, labels);
    }
}
