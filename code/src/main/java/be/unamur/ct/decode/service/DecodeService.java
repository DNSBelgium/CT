package be.unamur.ct.decode.service;

import be.unamur.ct.data.dao.CertificateDao;
import be.unamur.ct.decode.exceptions.NotAValidDomainException;
import be.unamur.ct.decode.model.Certificate;
import be.unamur.ct.download.model.LogEntry;
import be.unamur.ct.scrap.service.VatScrapingService;
import be.unamur.ct.scrap.thread.VatSearch;
import be.unamur.ct.thread.ThreadPool;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.operator.DefaultAlgorithmNameFinder;
import org.bouncycastle.util.encoders.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;


/**
 * Service class providing methods to decode certificates downloaded from
 * a Certificate Transparency log server in Base64 format
 */
@Service
public class DecodeService {

    @Autowired
    private CertificateDao certificateDao;

    @Autowired
    private VatScrapingService vatScrapingService;

    private final static Logger logger = LoggerFactory.getLogger(DecodeService.class);

    @Autowired
    private ThreadPool threadPool;

    // TODO use constructor injection
    public DecodeService() {}


    /**
     * Decode a single log entry (a single certificate) from its Base64 form to
     * a certificate as described in the Certificate class.
     * Once decoded, the certificate is saved in the database.
     *
     * @author Jules Dejaeghere
     * @param entry LogEntry object representing the Base64 downloaded certificate
     * @see Certificate
     */
    public void decodeToCert(LogEntry entry) {
        // Get an object from the previously saved list and extract certificate
        String leaf = entry.getLeaf();
        String extra = entry.getData();
        byte[] leafBin = Base64.decode(leaf);

        // Get certificate type (X.509 or PreCert)
        int id = (leafBin[11] & 0xFF) | ((leafBin[10] & 0xFF) << 8);
        int l = (leafBin[14] & 0xFF) | ((leafBin[13] & 0xFF) << 8) | ((leafBin[12] & 0x0F) << 16);

        if (id == 0) {

            try {
                // Extract only interesting part from certificate and create a X509CertificateHolder Object from it
                byte[] certBin = Arrays.copyOfRange(leafBin, 15, l + 15);
                X509CertificateHolder certX = new X509CertificateHolder(certBin);

                try {
                    String cns;
                    RDN cn;

                    // Get Subject
                    try {
                        cn = certX.getSubject().getRDNs(BCStyle.CN)[0];
                        cns = IETFUtils.valueToString(cn.getFirst().getValue());
                    } catch (IndexOutOfBoundsException e) {
                        //logger.warn("Cannot get domain name");
                        throw new NotAValidDomainException("createDomainFromCert(X509CertificateHolder) in DomainService: "
                                + "Cannot get domain name");
                    }

                    // TODO cns == null is always false
                    // TODO: check if exception is properly handled upstream

                    // Check TLD
                    if (cns == null || !(cns.endsWith(".be") || cns.endsWith(".vlaanderen") || cns.endsWith(".brussels"))) {
                        // TODO: should we really throw an exception every time when domain is not in one of our TLD's
                        throw new NotAValidDomainException("createDomainFromCert(X509CertificateHolder) in DomainService: "
                                + cn + " is not a valid domain");
                    }

                    // Create certificate
                    Certificate certificate = new Certificate(cns);

                    // Get root CA
                    String issuer = searchRoot(extra);
                    certificate.setIssuer(issuer);

                    certificate = setAttributes(certificate, certX);
                    certificate = certificateDao.save(certificate);

                    // NEXT STEP - Scrap for VAT
                    // TODO: avoid static method ?
                    // TODO: better decouple CT scraping from VAT scraping (by keeping state for each task in the DB ?)

                    ThreadPool.getVATScrapperExecutor().execute(new VatSearch(certificate, vatScrapingService));


                } catch (NotAValidDomainException e) {
                    // TODO
                    // logger.warn("NotAValidDomainException: {}", e.getMessage());
                }

            } catch (IOException e) {
                logger.warn("Cannot get certificate from binary");
            }
        }

    }


    /**
     * Returns a certificate filled with the validity period, the signature algorithm and the version number.
     * The details are extracted from a X509CertificateHolder object to be set in a Certificate object.
     *
     * @author Jules Dejaeghere
     * @param certificate Certificate object to be filled with the details
     * @param cert        X509CertificateHolder object representing the certificate to extract data from
     * @return The certificate filled with details
     */
    public Certificate setAttributes(Certificate certificate, X509CertificateHolder cert) {

        // Get validity period
        Date notBefore = cert.getNotBefore();
        certificate.setNotBefore(notBefore);
        Date notAfter = cert.getNotAfter();
        certificate.setNotAfter(notAfter);

        // Get Signature Algorithm
        AlgorithmIdentifier algoId = cert.getSignatureAlgorithm();
        DefaultAlgorithmNameFinder nameFinder = new DefaultAlgorithmNameFinder();
        String algoName = nameFinder.getAlgorithmName(algoId);
        certificate.setSignatureAlg(algoName);

        // Get Version Number
        int versionNumber = cert.getVersionNumber();
        certificate.setVersionNumber(versionNumber);

        return certificate;
    }


    /**
     * Search the root Certificate Authority (CA) from the downloaded certificate in its Base64 representation.
     * The Base64 data downloaded from the logs contains a chain of trust from the entity certified
     * by the certificate up to a CA accepted by the log.
     *
     * @author Jules Dejaeghere
     * @param extra_data extra_data field from the downloaded certificate, in Base64 representation
     * @return String containing the name of the CA
     * @throws NotAValidDomainException if no CA can be found while parsing the data
     */
    public String searchRoot(String extra_data) throws NotAValidDomainException {
        /*
         * Browse extra_data from the end to the beginning looking for a certificate.  The first certificate found is
         * the certificate of the CA as it is the last one of the structure that is browsed from the end to the start.
         *
         * TODO:
         *  find a better  way to determine the RootCA, the approach described above is probably not the best
         */

        byte[] extraBin = Base64.decode(extra_data);
        int start = extraBin.length - 5;

        while (start >= 0) {

            try {
                // Get certificate type (X.509 or PreCert)
                int id = (extraBin[start + 1] & 0xFF) | ((extraBin[start] & 0xFF) << 8);
                int l = (extraBin[start + 4] & 0xFF) | ((extraBin[start + 3] & 0xFF) << 8) | ((extraBin[start + 2] & 0x0F) << 16);

                byte[] certBin = Arrays.copyOfRange(extraBin, start + 5, l + start + 5);

                X509CertificateHolder certX = new X509CertificateHolder(certBin);
                RDN[] commonNames = certX.getSubject().getRDNs(BCStyle.CN);

                if (commonNames.length > 0) {
                    RDN cn = certX.getSubject().getRDNs(BCStyle.CN)[0];
                    return IETFUtils.valueToString(cn.getFirst().getValue());
                }
            } catch (Exception e) {
                logger.warn(e.toString());
            }
            start--;
        }
        throw new NotAValidDomainException("createDomainFromCert(X509CertificateHolder) in DomainService: " +
                "no CA found");
    }

}
