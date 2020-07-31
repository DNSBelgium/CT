package be.unamur.ct;


import be.unamur.ct.decode.model.Certificate;
import be.unamur.ct.decode.service.DecodeService;
import be.unamur.ct.download.model.LogEntry;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.util.encoders.Base64;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;


/*
 * The database specified in the application.properties file should be running in order to run these test.
 * These tests will initialize the complete ApplicationContext to run, including the database
 * No changes will be made to the database, the application will only try to connect
 */
@SpringBootTest
@RunWith(SpringRunner.class)
public class DecodeServiceTest {


    @Autowired
    private DecodeService decodeService;

    private LogEntry logEntry;


    @Before
    public void before(){
        logEntry = new LogEntry();
        logEntry.setId(1);
        logEntry.setLeaf("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");
        logEntry.setData("AAvuAAYMMIIGCDCCA/CgAwIBAgIQKy5u6tl1NmwUim7bo3yMBzANBgkqhkiG9w0BAQwFADCBhTELMAkGA1UEBhMCR0IxGzAZBgNVBAgTEkdyZWF0ZXIgTWFuY2hlc3RlcjEQMA4GA1UEBxMHU2FsZm9yZDEaMBgGA1UEChMRQ09NT0RPIENBIExpbWl0ZWQxKzApBgNVBAMTIkNPTU9ETyBSU0EgQ2VydGlmaWNhdGlvbiBBdXRob3JpdHkwHhcNMTQwMjEyMDAwMDAwWhcNMjkwMjExMjM1OTU5WjCBkDELMAkGA1UEBhMCR0IxGzAZBgNVBAgTEkdyZWF0ZXIgTWFuY2hlc3RlcjEQMA4GA1UEBxMHU2FsZm9yZDEaMBgGA1UEChMRQ09NT0RPIENBIExpbWl0ZWQxNjA0BgNVBAMTLUNPTU9ETyBSU0EgRG9tYWluIFZhbGlkYXRpb24gU2VjdXJlIFNlcnZlciBDQTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAI7CAhnhoFmk6zg1jSz9AdDTScBkxwtiBUUWOqigwAwCfx3M28ShbXcDow+G+eMGnD4LgYqbSRutA776S9uMIO3Vzl5ljj4Nr0zCsLdFXlIvNN5IJGS0Qa4Al/e+Z96e0HqnU4A7fK31llVvl0cKfIWLIpeNs4TgllfQcBhglo/uLQeTnaG6ytHNe+nEKpooIZFNb5JPJaXyejXdJtxGpdCsWTWM/06RQ1A/WZMebFEh7lgUq/51UHg+TLAchhP6a5i84DuUHoVS3AOTJBhuyydRReZw3iVDpA3hSqXttn7IzW3uLh0nc13cRTCAquOyQQuvvUSH2rnlG51/ruWFgqUCAwEAAaOCAWUwggFhMB8GA1UdIwQYMBaAFLuvfgI9+qbxPISOre44mOzZMjLUMB0GA1UdDgQWBBSQr2o6lFoL2JDqElZz30O0Oija5zAOBgNVHQ8BAf8EBAMCAYYwEgYDVR0TAQH/BAgwBgEB/wIBADAdBgNVHSUEFjAUBggrBgEFBQcDAQYIKwYBBQUHAwIwGwYDVR0gBBQwEjAGBgRVHSAAMAgGBmeBDAECATBMBgNVHR8ERTBDMEGgP6A9hjtodHRwOi8vY3JsLmNvbW9kb2NhLmNvbS9DT01PRE9SU0FDZXJ0aWZpY2F0aW9uQXV0aG9yaXR5LmNybDBxBggrBgEFBQcBAQRlMGMwOwYIKwYBBQUHMAKGL2h0dHA6Ly9jcnQuY29tb2RvY2EuY29tL0NPTU9ET1JTQUFkZFRydXN0Q0EuY3J0MCQGCCsGAQUFBzABhhhodHRwOi8vb2NzcC5jb21vZG9jYS5jb20wDQYJKoZIhvcNAQEMBQADggIBAE4rdk+SHGI2ibp3wScF9BzWRJ2pmj6q1WZmAT7qSeaiNbz69t2Vjpk1mA42GHWx3d1Qcnyu3HeIzg/3kCDKo2cuH1Z/e+FE6kKVxF0NAVBGFfKBiVlsit2M8RKhjTpCipj4SzR7JzsItG8kO3KdY3RYPBpsP0/HEZrIqPW1N+8QRcZs2eBelSaz662jue5/DJpmNXMyYE7l3YphLG5SEXdoltMYdVEVABt0iN3hxzgEQyjpFv3ZBdRdRydg1vs4O2xyopT4Qhrf7W8GjEXCBgCq5Ojc2bXhc3js9iPc0d1sjhqPpepUfJa3w/5Vjo1JXvxku88+vZbrac2/4EjxYoIQ5QxGV/Iz2tDIY+3GH5QFlkoakdH368+PUq4NCNk+qKBR6cGHdNXJ93SrLlP7u3r7l+L4HyaPs9Kg4DdbKDsx5Q5XLVq4rXmsXiBmGqW5prU5wfWYQ//u+aen/e7KJD2AFsQXj4rBYKEMrltDR5FL1ZoXX/nUh8HCjLfn4g8wGTeGrODcQgPmlKidrv0PJFGUzpII0fxQ8ANAe4hZ7Q7drNJ3gjTcBpUC2JD5Leo31Rpg0Gcg19hCC0Wvgmje3WYkN5AplBlGGSW4gNfL1IYoakRwJiNiqZ+Gb7+6kHDSVneFeO/qJakXzlByjAA6quPbYzSf+AZxAeKCINT+b72xAAXcMIIF2DCCA8CgAwIBAgIQTKr5yttjb+Af907YWwOGnTANBgkqhkiG9w0BAQwFADCBhTELMAkGA1UEBhMCR0IxGzAZBgNVBAgTEkdyZWF0ZXIgTWFuY2hlc3RlcjEQMA4GA1UEBxMHU2FsZm9yZDEaMBgGA1UEChMRQ09NT0RPIENBIExpbWl0ZWQxKzApBgNVBAMTIkNPTU9ETyBSU0EgQ2VydGlmaWNhdGlvbiBBdXRob3JpdHkwHhcNMTAwMTE5MDAwMDAwWhcNMzgwMTE4MjM1OTU5WjCBhTELMAkGA1UEBhMCR0IxGzAZBgNVBAgTEkdyZWF0ZXIgTWFuY2hlc3RlcjEQMA4GA1UEBxMHU2FsZm9yZDEaMBgGA1UEChMRQ09NT0RPIENBIExpbWl0ZWQxKzApBgNVBAMTIkNPTU9ETyBSU0EgQ2VydGlmaWNhdGlvbiBBdXRob3JpdHkwggIiMA0GCSqGSIb3DQEBAQUAA4ICDwAwggIKAoICAQCR6FSS0gpWsawNJN3Fz0RndJkrN6N9I3AAcbxT38T6KhKPS38QVr2fcHK3YX/JSw8Xpz3jsARh7v8Rl8f0hj4K+j5c+ZPmNHrZFGvnnLOFoIJ6dq9xkNfs/Q36nGz637CC9BR++b7Epi9Pf5l/tfxnQ3K9DADWietrLNPtj5gcFKt+5eNu/Nio5JIk2kNrYrhV/erBvGy2i/MOjZrkm2xpmfh4SDBF1a3hDTxFYPwyllEnvGfDyi62a+pGx8cgoLEfZd5ICLqkTqnyg0Y3hOvozIFIQ2dOciqbXL1MGyiKXCJ7tKuY2e7gUYPDCUZObT6Z+pUX2nwzV0E8jVHtC7ZcryxjGt9XyD+86V3Em69FmeKjWiS0uqlWPc9vqv9JWL7wqP/0uK3pN/u6uPQLOvnoQ0IeidiEyxPx2bvhiWC4jChWrBQdnArncevPDt09qZahSL0896+1DSJMwBGB7FY79tOi4lu3sgQiUpWAk2nojkxl8ZEDLXB0AuqLZxUpaVICu9ffUGpVRr+goyhhf3DQw6KqLCGqR84onAZFdr+CGCe01a60y1Dma/RMhnEw6abfFobg2P9A3fvQQoh/ozM6LlweQRGBY84YcWsr7KaKtzFcOmpH4MN5WdYgGq/yapiqcrxXStJLnbsQ/LBMQeXtHT1eKJ2czL+zUdqnR+WEUwIDAQABo0IwQDAdBgNVHQ4EFgQUu69+Aj36pvE8hI6t7jiY7NkyMtQwDgYDVR0PAQH/BAQDAgEGMA8GA1UdEwEB/wQFMAMBAf8wDQYJKoZIhvcNAQEMBQADggIBAArx1UaEt65Ru2yyTUEUAJNMnMvlwFTPoCWOAvn9sKIN9SCYPBMtrFaisNZ+EZLpLrqeLppysb0ZRGxhNaKatBYSaVqM4dc+pBroLwP0rmEdEBsqpIt6xf4FpuHA1sj+nq6PK7o9mfjYcwlYRm6mnPTXJ9OV2jeDchzTc+CiR5kDOF3VSXkAKRzH7JsgHAckaVd4sjn8OoSgtZx8jb8uk2IntznaFxiuvTwJaP+EmzzV1gsD41eeFPfR60/IvYcjt7ZJQ3mFXLrrkguhxuhoqEwWsRqZCuhTLJK7oQkYdQxlqHvLI7cawiiFwxv/0Cti76R7CZGYZ4wUAc1oBmpjIXUDgIiKboHGhfKppC3n9KUkEEeDys30jXlYsQab5xoq2Z0B15R97QNKyvDb6KkBPvVWmckejkk9u+UJueBPSZI9FoJAzMxZxuY67RIuaTxslbH9qh17f4a+Hg4yRvv7E491f0yLS0Zj/gA0QHDBw7mh3aZw4gSzQbzpgJHqZJx64SIDqZxubw5lT2yHh17zbqD5daWbQOhTsiedSrnAdyGN/4fy3ryM7xfft0kL0fJuMAsaDk527RH89elWsn2/x20Kk4yl0MC2Hb46TpSi125sC8KKfPog88Tk5c0NqMuRkrF8hey1FGlmDoLnzc7ILaZRfyHBNVOFBkpdn627G190");
    }

    @Test
    public void testIndexOutOfBounds() throws IOException {
        // This test was used to understand the IndexOutOfBoundsException
        //  in be.unamur.ct.decode.service.DecodeService.decodeToCert
        String leaf = "AAAAAAFmMOHAZgAAAATzMIIE7zCCAtegAwIBAgIHBXHAmx3S8TANBgkqhkiG9w0BAQsFADB/MQswCQYDVQQGEwJHQjEPMA0GA1UECAwGTG9uZG9uMRcwFQYDVQQKDA5Hb29nbGUgVUsgTHRkLjEhMB8GA1UECwwYQ2VydGlmaWNhdGUgVHJhbnNwYXJlbmN5MSMwIQYDVQQDDBpNZXJnZSBEZWxheSBJbnRlcm1lZGlhdGUgMTAeFw0xODA3MjQxNTQyMTNaFw0yMDA3MTEyMzEzMzFaMGMxCzAJBgNVBAYTAkdCMQ8wDQYDVQQHDAZMb25kb24xKDAmBgNVBAoMH0dvb2dsZSBDZXJ0aWZpY2F0ZSBUcmFuc3BhcmVuY3kxGTAXBgNVBAUTEDE1MzI0NDY5MzM2MzU4MjUwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQDSOkvnQcpt92qQtuASdqw2KFP0sA68/pPF50uHFIkwQDfufCZcFf9DssEFhJKNTsWzKEiDI7aiTHH1IMRh5ewFubUT+f+mduEs8h4GimW19TLi6LanXDOiNDSRXa2y4KgaYgq/eaJyuFRb34K+A6+iPpkTy33y7pVCR9eQsCoNmu8VB9vw/7rR02kX5HbiXCPWHbnjahqgBBtsgz1B+XBJSdFAG3mIFIw03abJKVDdq8erL9l3HERjy5rziKwO7iPJJHeLAdkcVC/4VZd3NIHyezTQigFDjbpHzlkpGx5RcB3YK1gJ5jAzpeonUvqeIhVT5LYYDxnXgPfyKdB95xb1AgMBAAGjgYswgYgwEwYDVR0lBAwwCgYIKwYBBQUHAwEwIwYDVR0RBBwwGoIYZmxvd2Vycy10by10aGUtd29ybGQuY29tMAwGA1UdEwEB/wQCMAAwHwYDVR0jBBgwFoAU6TwE4YAvwoQTLSZwnvL9Gs+q/sYwHQYDVR0OBBYEFEuafVrqd785+jkTM8pc9BGAOrnIMA0GCSqGSIb3DQEBCwUAA4ICAQACvof6TLlIL/1Q4hARAqevOEMRzjfNoef0Cjj9zgRP8ViZ6doePGxnz502DzyKgmiHoUIhhvpRTmFx0xNAovXxhr6D1GFgWeBA+XLOxffW8ceLdKj6Ej22v7tg+u4uazElWPVFVxjCGPYmpyM2yWVZCQFCEouIRyPGdlpK5JxX4vJbseUQPG+Iu+SxN9Vb4icME2L9siQ6vrebBnonXjWOAtL8tuaYIFZ+vrOyRAbwZyIgtZNP76mjKiAdpVz2lkJt312vY6vkV9889FcFiFJ+QJ9s8IwXOGjxaK+v/Bn+6GkvbRz6gh15Ls50e8Q4G/IApbAr4qgziEqIo46eZx60Y4u1xBoRHt9jNUhkSDO8ulPQcczde6SMTv2/i7/XGO8P/hu1LoukMqgordEduDes5hQLb2nN3QAsoH7spGUm/Rk5pKMIGV2ajjjDir4vZpcnHJgKZRIewbAaI/ODPMcEmsybgaviRcF/0GKYb9Hat2NV5ZDP7K9aWrUnODPZ9UCr3/lScOHCAiZDzdDoGhb2Hq2Ke4UEMqeJoFZtyziPFXysjxiRUOWk1Ge0br5fuDoZ1l/tXOqA6f3nCWNQZ7CWd2b0mzvJ6gep9tPkoilNiA/pJeKuXZviYFqr5De9NLxX71NtyUYCK+LfnpCTeEEEAE5ByQZLcGT/9kJLk5p8HwAA";
        byte[] leafBin = Base64.decode(leaf);

        // Get certificate type (X.509 or PreCert)
        int id = (leafBin[11] & 0xFF) | ((leafBin[10] & 0xFF) << 8);
        int l = (leafBin[14] & 0xFF) | ((leafBin[13] & 0xFF) << 8) | ((leafBin[12] & 0x0F) << 16);

        byte[] certBin = Arrays.copyOfRange(leafBin, 15, l + 15);
        X509CertificateHolder certX = new X509CertificateHolder(certBin);

        X500Name subject = certX.getSubject();
        System.out.println("subject = " + subject);
        RDN[] commonNames = certX.getSubject().getRDNs(BCStyle.CN);
        if (commonNames.length > 0) {
            RDN cn = certX.getSubject().getRDNs(BCStyle.CN)[0];
            String cns = IETFUtils.valueToString(cn.getFirst().getValue());
            System.out.println("cns = " + cns);
        } else {
            System.out.println("No common name found");
        }

    }

    @Test
    public void testSearchRootAndSetAttributes() throws IOException {
        // Get an object from the previously saved list and extract certificate
        String leaf = logEntry.getLeaf();
        String extra = logEntry.getData();
        byte[] leafBin = Base64.decode(leaf);

        // Get certificate type (X.509 or PreCert)
        int l = (leafBin[14] & 0xFF) | ((leafBin[13] & 0xFF) << 8) | ((leafBin[12] & 0x0F) << 16);

        // Extract only interesting part from certificate and create a X509CertificateHolder Object from it
        byte[] certBin = Arrays.copyOfRange(leafBin, 15, l + 15);
        X509CertificateHolder certX = new X509CertificateHolder(certBin);


        String cns;
        RDN cn;

        // Get Subject
        cn = certX.getSubject().getRDNs(BCStyle.CN)[0];
        cns = IETFUtils.valueToString(cn.getFirst().getValue());


        // Create certificate
        Certificate certificate = new Certificate(cns);

        // Get root CA
        String issuer = decodeService.searchRoot(extra);
        certificate.setIssuer(issuer);

        certificate =  decodeService.setAttributes(certificate, certX);

        assertThat(certificate.getIssuer()).isEqualTo("COMODO RSA Certification Authority");
        assertThat(certificate.getSubject()).isEqualTo("www.vprmedia.be");
        assertThat(certificate.getSignatureAlg()).isEqualTo("SHA256WITHRSA");

    }

}
