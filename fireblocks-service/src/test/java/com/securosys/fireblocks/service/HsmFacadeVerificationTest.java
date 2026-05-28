// SPDX-FileCopyrightText: Copyright 2026 Securosys SA
// SPDX-License-Identifier: Apache-2.0

package com.securosys.fireblocks.service;

import com.securosys.fireblocks.business.dto.ServiceName;
import com.securosys.fireblocks.business.facade.HsmFacade;
import com.securosys.fireblocks.business.service.TsbService;
import com.securosys.fireblocks.business.util.CryptoUtil;
import com.securosys.fireblocks.configuration.CustomServerProperties;
import com.securosys.fireblocks.configuration.TsbProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HsmFacadeVerificationTest {

    private static final String PAYLOAD = "{\"hello\":\"fireblocks\"}";

    @Mock
    private TsbService tsbService;

    @Mock
    private TsbProperties tsbProperties;

    @Mock
    private CryptoUtil cryptoUtil;

    private CustomServerProperties properties;
    private HsmFacade hsmFacade;

    @BeforeEach
    void setUp() {
        properties = new CustomServerProperties();
        hsmFacade = new HsmFacade(tsbService, properties, tsbProperties, cryptoUtil);
    }

    @Test
    @DisplayName("2.4.1.24 SIGNING_SERVICE verification accepts any whitelisted certificate")
    void verifySigningService_acceptsSecondWhitelistedCertificate() throws Exception {
        KeyPair firstKey = generateRsaKeyPair();
        KeyPair secondKey = generateRsaKeyPair();

        properties.setSignatureServices(Map.of(
                "signingService", serviceConfig("SIGNING_SERVICE", true, "first-cert", "second-cert")
        ));
        mockCertificate("first-cert", firstKey.getPublic());
        mockCertificate("second-cert", secondKey.getPublic());

        boolean verified = hsmFacade.verify(
                sign(PAYLOAD, secondKey),
                ServiceName.SIGNING_SERVICE,
                PAYLOAD
        );

        assertThat(verified).isTrue();
    }

    @Test
    @DisplayName("2.4.1.25 CONFIGURATION_MANAGER verification is isolated from SIGNING_SERVICE certificates")
    void verifyConfigurationManager_doesNotUseSigningServiceCertificates() throws Exception {
        KeyPair signingServiceKey = generateRsaKeyPair();
        KeyPair configurationManagerKey = generateRsaKeyPair();

        Map<String, CustomServerProperties.FireblocksSignatureService> services = new LinkedHashMap<>();
        services.put("signingService", serviceConfig("SIGNING_SERVICE", true, "signing-cert"));
        services.put("configurationManager", serviceConfig("CONFIGURATION_MANAGER", true, "configuration-cert"));
        properties.setSignatureServices(services);
        mockCertificate("configuration-cert", configurationManagerKey.getPublic());

        boolean verified = hsmFacade.verify(
                sign(PAYLOAD, signingServiceKey),
                ServiceName.CONFIGURATION_MANAGER,
                PAYLOAD
        );

        assertThat(verified).isFalse();
        verify(cryptoUtil, never()).loadCertificate("signing-cert");
    }

    @Test
    @DisplayName("2.4.1.26 Disabled service verification does not fall back to legacy config")
    void verifyDisabledService_doesNotFallBackToLegacyConfig() throws Exception {
        KeyPair legacyKey = generateRsaKeyPair();

        properties.setServiceName("CONFIGURATION_MANAGER");
        properties.setFireblocksSignatureCertificate("legacy-cert");
        properties.setSignatureServices(Map.of(
                "configurationManager", serviceConfig("CONFIGURATION_MANAGER", false, "configuration-cert")
        ));

        boolean verified = hsmFacade.verify(
                sign(PAYLOAD, legacyKey),
                ServiceName.CONFIGURATION_MANAGER,
                PAYLOAD
        );

        assertThat(verified).isFalse();
        verify(cryptoUtil, never()).loadCertificate("legacy-cert");
    }

    @Test
    @DisplayName("2.4.1.27 Legacy single-service verification config still works")
    void verifyLegacySingleServiceConfig_stillWorks() throws Exception {
        KeyPair legacyKey = generateRsaKeyPair();

        properties.setServiceName("SIGNING_SERVICE");
        properties.setFireblocksSignatureCertificate("legacy-cert");
        mockCertificate("legacy-cert", legacyKey.getPublic());

        boolean verified = hsmFacade.verify(
                sign(PAYLOAD, legacyKey),
                ServiceName.SIGNING_SERVICE,
                PAYLOAD
        );

        assertThat(verified).isTrue();
    }

    private CustomServerProperties.FireblocksSignatureService serviceConfig(String serviceName, boolean enabled, String... certificates) {
        CustomServerProperties.FireblocksSignatureService service = new CustomServerProperties.FireblocksSignatureService();
        service.setEnabled(enabled);
        service.setServiceName(serviceName);
        service.setFireblocksSignatureCertificates(List.of(certificates));
        return service;
    }

    private void mockCertificate(String location, PublicKey publicKey) {
        X509Certificate certificate = mock(X509Certificate.class);
        when(certificate.getPublicKey()).thenReturn(publicKey);
        when(cryptoUtil.loadCertificate(location)).thenReturn(certificate);
    }

    private KeyPair generateRsaKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private byte[] sign(String payload, KeyPair keyPair) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(keyPair.getPrivate());
        signature.update(payload.getBytes(StandardCharsets.UTF_8));
        return signature.sign();
    }
}
