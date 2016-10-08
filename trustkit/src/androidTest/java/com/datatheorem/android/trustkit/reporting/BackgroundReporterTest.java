package com.datatheorem.android.trustkit.reporting;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.datatheorem.android.trustkit.CertificateUtils;
import com.datatheorem.android.trustkit.PinningValidationResult;
import com.datatheorem.android.trustkit.TestableTrustKit;
import com.datatheorem.android.trustkit.config.DomainPinningPolicy;
import com.datatheorem.android.trustkit.reporting.BackgroundReporter;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.MalformedURLException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.UUID;


@RunWith(AndroidJUnit4.class)
public class BackgroundReporterTest {

    private final String intermediatePem =
            "MIID8DCCAtigAwIBAgIDAjqSMA0GCSqGSIb3DQEBCwUAMEIxCzAJBgNVBAYTAlVT\n" +
                    "MRYwFAYDVQQKEw1HZW9UcnVzdCBJbmMuMRswGQYDVQQDExJHZW9UcnVzdCBHbG9i\n" +
                    "YWwgQ0EwHhcNMTUwNDAxMDAwMDAwWhcNMTcxMjMxMjM1OTU5WjBJMQswCQYDVQQG\n" +
                    "EwJVUzETMBEGA1UEChMKR29vZ2xlIEluYzElMCMGA1UEAxMcR29vZ2xlIEludGVy\n" +
                    "bmV0IEF1dGhvcml0eSBHMjCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEB\n" +
                    "AJwqBHdc2FCROgajguDYUEi8iT/xGXAaiEZ+4I/F8YnOIe5a/mENtzJEiaB0C1NP\n" +
                    "VaTOgmKV7utZX8bhBYASxF6UP7xbSDj0U/ck5vuR6RXEz/RTDfRK/J9U3n2+oGtv\n" +
                    "h8DQUB8oMANA2ghzUWx//zo8pzcGjr1LEQTrfSTe5vn8MXH7lNVg8y5Kr0LSy+rE\n" +
                    "ahqyzFPdFUuLH8gZYR/Nnag+YyuENWllhMgZxUYi+FOVvuOAShDGKuy6lyARxzmZ\n" +
                    "EASg8GF6lSWMTlJ14rbtCMoU/M4iarNOz0YDl5cDfsCx3nuvRTPPuj5xt970JSXC\n" +
                    "DTWJnZ37DhF5iR43xa+OcmkCAwEAAaOB5zCB5DAfBgNVHSMEGDAWgBTAephojYn7\n" +
                    "qwVkDBF9qn1luMrMTjAdBgNVHQ4EFgQUSt0GFhu89mi1dvWBtrtiGrpagS8wDgYD\n" +
                    "VR0PAQH/BAQDAgEGMC4GCCsGAQUFBwEBBCIwIDAeBggrBgEFBQcwAYYSaHR0cDov\n" +
                    "L2cuc3ltY2QuY29tMBIGA1UdEwEB/wQIMAYBAf8CAQAwNQYDVR0fBC4wLDAqoCig\n" +
                    "JoYkaHR0cDovL2cuc3ltY2IuY29tL2NybHMvZ3RnbG9iYWwuY3JsMBcGA1UdIAQQ\n" +
                    "MA4wDAYKKwYBBAHWeQIFATANBgkqhkiG9w0BAQsFAAOCAQEACE4Ep4B/EBZDXgKt\n" +
                    "10KA9LCO0q6z6xF9kIQYfeeQFftJf6iZBZG7esnWPDcYCZq2x5IgBzUzCeQoY3IN\n" +
                    "tOAynIeYxBt2iWfBUFiwE6oTGhsypb7qEZVMSGNJ6ZldIDfM/ippURaVS6neSYLA\n" +
                    "EHD0LPPsvCQk0E6spdleHm2SwaesSDWB+eXknGVpzYekQVA/LlelkVESWA6MCaGs\n" +
                    "eqQSpSfzmhCXfVUDBvdmWF9fZOGrXW2lOUh1mEwpWjqN0yvKnFUEv/TmFNWArCbt\n" +
                    "F4mmk2xcpMy48GaOZON9muIAs0nH5Aqq3VuDx3CQRk6+0NtZlmwu9RY23nHMAcIS\n" +
                    "wSHGFg==";
    private final Certificate intermediate = CertificateUtils.certificateFromPem(intermediatePem);

    private final String leafPem =
            "MIID1jCCAr6gAwIBAgIIAPCznYJ9GMQwDQYJKoZIhvcNAQELBQAwSTELMAkGA1UE\n" +
                    "BhMCVVMxEzARBgNVBAoTCkdvb2dsZSBJbmMxJTAjBgNVBAMTHEdvb2dsZSBJbnRl\n" +
                    "cm5ldCBBdXRob3JpdHkgRzIwHhcNMTYwOTI5MTcwMjI5WhcNMTYxMjIyMTYzNzAw\n" +
                    "WjBpMQswCQYDVQQGEwJVUzETMBEGA1UECAwKQ2FsaWZvcm5pYTEWMBQGA1UEBwwN\n" +
                    "TW91bnRhaW4gVmlldzETMBEGA1UECgwKR29vZ2xlIEluYzEYMBYGA1UEAwwPbWFp\n" +
                    "bC5nb29nbGUuY29tMFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE8W4z2S50gAvv\n" +
                    "0VC6l7isbjD0Q7d7BiKWeOQwqfY+dLTmxZvpxBpcrfPlh170R3ai+qn/BE7t4+k2\n" +
                    "a+LrfYag8KOCAWswggFnMB0GA1UdJQQWMBQGCCsGAQUFBwMBBggrBgEFBQcDAjAs\n" +
                    "BgNVHREEJTAjgg9tYWlsLmdvb2dsZS5jb22CEGluYm94Lmdvb2dsZS5jb20wCwYD\n" +
                    "VR0PBAQDAgeAMGgGCCsGAQUFBwEBBFwwWjArBggrBgEFBQcwAoYfaHR0cDovL3Br\n" +
                    "aS5nb29nbGUuY29tL0dJQUcyLmNydDArBggrBgEFBQcwAYYfaHR0cDovL2NsaWVu\n" +
                    "dHMxLmdvb2dsZS5jb20vb2NzcDAdBgNVHQ4EFgQUTuRBMq9TH5Dh82EGV6U37V8b\n" +
                    "3AcwDAYDVR0TAQH/BAIwADAfBgNVHSMEGDAWgBRK3QYWG7z2aLV29YG2u2IaulqB\n" +
                    "LzAhBgNVHSAEGjAYMAwGCisGAQQB1nkCBQEwCAYGZ4EMAQICMDAGA1UdHwQpMCcw\n" +
                    "JaAjoCGGH2h0dHA6Ly9wa2kuZ29vZ2xlLmNvbS9HSUFHMi5jcmwwDQYJKoZIhvcN\n" +
                    "AQELBQADggEBADqnYrHvHoCc7ltooq4XVj3yEyFX+n/hgrdQMmOgVcl3bHNYV5EG\n" +
                    "IqOClo5g1RyWcRfji8RQGv0hvFb6L2Zef5sOpQs3COEVW05kmCdwWSlCCpp6pJma\n" +
                    "yf6Nf4TreI8gpokoJgTNNgmq5OgT9K+G16I2L/CKv8rTh9HaoOOXWx90s5rAn/G/\n" +
                    "JrRRcgICjonU7m+ab22vdVilOJlEuMdX7x1CBPtHY/c214oJ32AxSTewXUjjDsWY\n" +
                    "c2azHgSpG5uA/TocrImlvajjrdZwQjtj8wO4av35BdaaOlfMo/xqa6VQfpA6W9ml\n" +
                    "jTL1zvT0Sv8mKow3b3blztbbeaVifTHShrA=";
    private final Certificate leaf = CertificateUtils.certificateFromPem(leafPem);

    @Before
    public void setUp() throws Exception {
        TestableTrustKit.reset();
    }

    @Test
    public void testPinValidationFailed() throws MalformedURLException {
        // Initialize TrustKit
        String serverHostname = "mail.google.com";
        final DomainPinningPolicy domainPolicy = new DomainPinningPolicy.Builder()
                .setHostname(serverHostname)
                .setShouldEnforcePinning(true)
                .setPublicKeyHashes(new HashSet<String>() {{
                    // Wrong pins
                    add("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=");
                    add("BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB=");
                }}).build();

        TestableTrustKit.init(new HashSet<DomainPinningPolicy>() {{ add(domainPolicy); }},
                InstrumentationRegistry.getContext(),
                null);

        BackgroundReporter reporter = new BackgroundReporter(true, "com.unit.tests", "1.2",
                TestableTrustKit.getOrCreateVendorIdentifier(InstrumentationRegistry.getContext()));

        ArrayList<X509Certificate> certChain = new ArrayList<>();
        certChain.add((X509Certificate)leaf);
        certChain.add((X509Certificate)intermediate);

        reporter.pinValidationFailed("mail.google.com", 443, certChain, certChain,
                TestableTrustKit.getInstance().getConfiguration()
                .getConfigForHostname("mail.google.com"),
                PinningValidationResult.FAILED);
    }
}


