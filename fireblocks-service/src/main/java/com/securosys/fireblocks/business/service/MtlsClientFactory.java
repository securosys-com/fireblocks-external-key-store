// SPDX-FileCopyrightText: Copyright 2025 Securosys SA
// SPDX-License-Identifier: Apache-2.0

package com.securosys.fireblocks.business.service;

import com.securosys.fireblocks.business.exceptions.BusinessException;
import com.securosys.fireblocks.business.exceptions.BusinessReason;
import com.securosys.fireblocks.configuration.TsbProperties;
import com.securosys.fireblocks.business.util.ConfigUtil;
import com.securosys.fireblocks.business.util.CryptoUtil;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.stereotype.Component;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MtlsClientFactory {

    private final ConfigUtil configUtil;
    private final TsbProperties tsbProperties;

    private CloseableHttpClient httpClient;

    @PostConstruct
    public void init() {
        if (isMtlsEnabled()) {
            try {
                log.info("Initializing mTLS HTTP client");
                this.httpClient = createHttpClient();
            } catch (Exception e) {
                log.error("Failed to initialize mTLS HTTP client", e);
                throw new BusinessException("Could not create mTLS HttpClient", BusinessReason.ERROR_GENERAL);
            }
        } else {
            log.info("mTLS not configured, skipping HttpClient initialization");
        }
    }
    public CloseableHttpClient getClient() {
        if (httpClient != null) {
            return httpClient;
        }
        throw new BusinessException("mTLS HttpClient is not initialized", BusinessReason.ERROR_GENERAL);
    }


    private CloseableHttpClient createHttpClient() {
            try {
                Security.addProvider(new BouncyCastleProvider());
                log.info("Initializing HTTP client with mTLS support...");
                X509Certificate certificate = loadX509CertificateFromConfig();
                PrivateKey privateKey = loadPrivateKey();

                SSLContext sslContext = createSslContext(certificate, privateKey);

                DefaultClientTlsStrategy tlsStrategy = new DefaultClientTlsStrategy(sslContext);

                HttpClientConnectionManager connMgr = PoolingHttpClientConnectionManagerBuilder.create()
                        .setTlsSocketStrategy(tlsStrategy)
                        .build();

                log.info("mTLS HttpClient initialized");

                return HttpClients.custom()
                        .setConnectionManager(connMgr)
                        .build();

            } catch (Exception e) {
                throw new BusinessException("Failed to initialize mTLS client: " + e, BusinessReason.ERROR_GENERAL);
            }
    }

    private boolean isMtlsEnabled() {
        return !isBlank(tsbProperties.getTsbMTlsCertificate()) && !isBlank(tsbProperties.getTsbMTlsKey());
    }

    private X509Certificate loadX509CertificateFromConfig() {
        log.debug("Loading X509 certificate from config");
        String x509CertificateReference = tsbProperties.getTsbMTlsCertificate();
        File x509CertificatePem = configUtil.readFile(x509CertificateReference);
        return CryptoUtil.parseX509CertificateFromFile(x509CertificatePem);
    }

    /**
     * Loads a private key from a specified file location
     * @return The private key object
     */
    public PrivateKey loadPrivateKey() {
        String privateKeyLocation = tsbProperties.getTsbMTlsKey();
        File privateKey = configUtil.readFile(privateKeyLocation);
        Path keyPath = privateKey.toPath();
        try {
            String keyContent = Files.readString(keyPath);
            return parsePrivateKey(keyContent);
        } catch (IOException e){
            throw new BusinessException("Could not read mTls key file", BusinessReason.ERROR_CONFIG_NOT_VALID);
        }
    }

    private static PrivateKey parsePrivateKey(String privateKey) {
        byte[] decodedKey = Base64.getDecoder().decode(privateKey.replaceAll("-----.*-----", "").replaceAll("\n", ""));
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decodedKey);
        for (String algorithm : List.of("RSA", "EC", "DSA", "Ed25519", "Ed448", "X25519", "X448")) {
            try {
                KeyFactory kf = KeyFactory.getInstance(algorithm);
                return kf.generatePrivate(keySpec);
            } catch (Exception e) {
                // Try next algorithm
            }
        }
        throw new BusinessException("Unsupported key algorithm.", BusinessReason.ERROR_INVALID_CRYPTO_FILE);
    }

    private SSLContext createSslContext(X509Certificate cert, PrivateKey key) throws Exception {
        Security.addProvider(new BouncyCastleProvider());

        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null, null);
        trustStore.setCertificateEntry("trustedCert", cert);

        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(trustStore);


        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);
        keyStore.setCertificateEntry("clientCert", cert);
        keyStore.setKeyEntry("clientKey", key, null, new X509Certificate[]{cert});

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, null);

        return SSLContextBuilder.create()
                .setProtocol("TLS")
                .loadKeyMaterial(keyStore, null)
                .loadTrustMaterial(trustStore, (chain, authType) -> true)
                .build();
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
