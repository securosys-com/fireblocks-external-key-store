// SPDX-FileCopyrightText: Copyright 2025 Securosys SA
// SPDX-License-Identifier: Apache-2.0

package com.securosys.fireblocks.business.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.securosys.fireblocks.business.dto.response.KeyAttributesDto;
import com.securosys.fireblocks.business.dto.response.LicenseResponseDto;
import com.securosys.fireblocks.business.dto.response.RequestStatusResponseDto;
import com.securosys.fireblocks.business.exceptions.BusinessException;
import com.securosys.fireblocks.business.exceptions.BusinessReason;
import com.securosys.fireblocks.configuration.ApiKeyTypes;
import com.securosys.fireblocks.configuration.TsbProperties;
import com.securosys.fireblocks.business.dto.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequest;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TsbService {

    private final TsbProperties tsbProperties;
    private final MtlsClientFactory mtlsClientFactory;
    private final AuthState authState;
    private final CloseableHttpClient httpClient;
    private final ObjectMapper objectMapper;

    private static final String KEY_ALGORITHM = "EC";
    private static final String KEY_OID = "1.3.132.0.10";
    private static final int VALIDITY = 365;

    private static final int ERROR_INVALID_API_KEY = 631;

    private static final Map<String, BusinessReason> REASON_BY_NAME =
            Arrays.stream(BusinessReason.values())
                    .collect(Collectors.toMap(BusinessReason::getReason, r -> r));

    public void rollOverApiKey(String name) {
        switch (name) {
            case Constants.KEY_MANAGEMENT_TOKEN_NAME -> authState.incrementKeyManagementIndex();
            case Constants.KEY_OPERATION_TOKEN_NAME -> authState.incrementKeyOperationIndex();
            case Constants.KEY_SERVICE_TOKEN_NAME -> authState.incrementKeyServiceIndex();
            default -> throw new BusinessException("Unsupported API key name: " + name, BusinessReason.ERROR_CONFIG_NOT_VALID);
        }
    }

    public boolean canGetNewApiKeyByName(String name) {
        ApiKeyTypes keys = tsbProperties.getApiAuthentication();
        if (keys == null) return false;
        if (!tsbProperties.getApiAuthentication().isEnabled()) return false;

        return switch (name) {
            case Constants.KEY_MANAGEMENT_TOKEN_NAME -> {
                List<String> tokens = keys.getKeyManagementToken();
                yield tokens != null
                        && tokens.stream().anyMatch(s -> s != null && !s.isBlank())
                        && authState.getKeyManagementTokenIndex() < tokens.size();
            }
            case Constants.KEY_OPERATION_TOKEN_NAME -> {
                List<String> tokens = keys.getKeyOperationToken();
                yield tokens != null
                        && tokens.stream().anyMatch(s -> s != null && !s.isBlank())
                        && authState.getKeyOperationTokenIndex() < tokens.size();
            }
            case Constants.KEY_SERVICE_TOKEN_NAME -> {
                List<String> tokens = keys.getServiceToken();
                yield tokens != null
                        && tokens.stream().anyMatch(s -> s != null && !s.isBlank())
                        && authState.getKeyServiceTokenIndex() < tokens.size();
            }
            default -> false;
        };
    }

    @NonNull
    public String getApiKeyByName(String name) {
        ApiKeyTypes keys = tsbProperties.getApiAuthentication();
        if (keys == null) return "";

        if (!canGetNewApiKeyByName(name)) return "";

        return switch (name) {
            case Constants.KEY_MANAGEMENT_TOKEN_NAME -> keys.getKeyManagementToken().get(authState.getKeyManagementTokenIndex());
            case Constants.KEY_OPERATION_TOKEN_NAME -> keys.getKeyOperationToken().get(authState.getKeyOperationTokenIndex());
            case Constants.KEY_SERVICE_TOKEN_NAME -> keys.getServiceToken().get(authState.getKeyServiceTokenIndex());
            default -> "";
        };
    }

    private boolean isMtlsEnabled() {
        return tsbProperties.getTsbMTlsCertificate() != null &&
                !tsbProperties.getTsbMTlsCertificate().isBlank() &&
                tsbProperties.getTsbMTlsKey() != null &&
                !tsbProperties.getTsbMTlsKey().isBlank();
    }

    public Map<String, Object> doRequest(HttpUriRequest request, String apiKeyName) {
        String accessToken = tsbProperties.getTsbAccessToken();
        if (accessToken != null && !accessToken.isBlank()) {
            request.setHeader("Authorization", "Bearer " + accessToken);
        }

        CloseableHttpClient client = isMtlsEnabled()
                ? mtlsClientFactory.getClient()
                : httpClient;

        boolean retry;

        do {
            retry = false;

            String apiKey = getApiKeyByName(apiKeyName);
            if (!apiKey.isBlank()) {
                request.setHeader("X-API-KEY", apiKey);
            } else {
                request.removeHeaders("X-API-KEY");
            }

            try (CloseableHttpResponse response = client.execute(request)) {

                int statusCode = response.getCode();
                String responseBody = response.getEntity() != null
                        ? EntityUtils.toString(response.getEntity())
                        : "";

                if (!apiKey.isBlank() && statusCode == HttpURLConnection.HTTP_UNAUTHORIZED) {

                    JsonNode jsonObject = null;
                    try {
                        jsonObject = objectMapper.readTree(responseBody);
                    } catch (Exception ignored) {}

                    if (jsonObject != null
                            && jsonObject.has("errorCode")
                            && jsonObject.get("errorCode").asInt() == ERROR_INVALID_API_KEY
                            && canGetNewApiKeyByName(apiKeyName)) {

                        rollOverApiKey(apiKeyName);
                        retry = true;
                        continue;
                    }
                }

                Map<String, Object> responseMap = new HashMap<>();
                responseMap.put("statusCode", statusCode);
                responseMap.put("body", responseBody);
                return responseMap;

            } catch (ParseException | IOException e) {
                throw new BusinessException(
                        "Error executing request",
                        BusinessReason.ERROR_IN_SUBSYSTEM,
                        e
                );
            }

        } while (retry);

        try (CloseableHttpResponse response = client.execute(request)) {

            int statusCode = response.getCode();
            String responseBody = response.getEntity() != null
                    ? EntityUtils.toString(response.getEntity())
                    : "";

            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("statusCode", statusCode);
            responseMap.put("body", responseBody);
            return responseMap;

        } catch (ParseException | IOException e) {
            throw new BusinessException("Error executing request", BusinessReason.ERROR_IN_SUBSYSTEM);
        }

    }


    /**
     * Returns the status of a request with the specified request ID from the TSB service.
     *
     * <p>This method sends a GET request to retrieve the status of a specific request from the TSB. If the response
     * indicates unauthorized access or another error, appropriate exceptions are thrown. The response body is parsed
     * into a {@link RequestStatusResponseDto} object.</p>
     *
     * @param requestId the ID of the request to retrieve the status for
     * @return a {@link RequestStatusResponseDto} object containing the status of the request
     */
    public RequestStatusResponseDto getRequest(String requestId) {

        final HttpGet request = new HttpGet(tsbProperties.getTsbRestApi() + "/v1/request/" + requestId);
        request.addHeader("Content-Type", "application/json");

        try {
            final Map<String, Object> responseMap = doRequest(request, Constants.KEY_OPERATION_TOKEN_NAME);

            final int statusCode = (int) responseMap.get("statusCode");
            final String responseBody = (String) responseMap.get("body");

            if (log.isDebugEnabled()) {
                log.debug("Response Securosys TSB get request: {}", responseBody);
            }

            if (statusCode != HttpURLConnection.HTTP_OK) {
                handleErrorResponse(statusCode, responseBody);
            }

            final JsonNode responseData = objectMapper.readTree(responseBody);

            RequestStatusResponseDto requestBody = new RequestStatusResponseDto();
            requestBody.setId(responseData.get("id").asText());
            requestBody.setStatus(responseData.get("status").asText());
            requestBody.setResult(responseData.get("result").asText());
            if (log.isDebugEnabled()) {
                log.debug("Status code get request is: {}", statusCode);
                log.debug("Response JSON: {}", responseData);
            }
            return requestBody;
        } catch (IOException e) {
            throw new BusinessException("Failed to get request status", BusinessReason.ERROR_GENERAL);
        }

    }

    /**
     * Creates a new key or updates the parameters of an existing key on the TSB service.
     *
     * <p>This method sends a POST request to either create a new key or update an existing key's parameters on the TSB
     * service. The request body includes details such as label, password, attributes, key type, key size, and policy.
     * If the response indicates unauthorized access or another error, appropriate exceptions are thrown.</p>
     *
     * @param label    the label of the key
     * @param password the password for the key, if applicable
     * @param keyType  the type of the key (e.g., "RSA", "EC")
     * @param curveOid  the OID of the key
     */
    public void createOrUpdateKey(String label, String password, String keyType, String curveOid, int keySize) {
        final ObjectNode jsonBody = objectMapper.createObjectNode();
        jsonBody.put("label", label);
        jsonBody.put("algorithm", keyType);

        if (keySize > 0) {
            jsonBody.put("keySize", keySize);
        }

        if (password != null && (!password.isEmpty())) {
            jsonBody.put("password", Arrays.toString(password.toCharArray()));
        }

        if (curveOid != null && (!curveOid.isEmpty())) {
            jsonBody.put("curveOid", curveOid);
        }

        final Map<String, Boolean> attributes = prepareAttributes();

        final ObjectNode attributesNode = objectMapper.convertValue(attributes, ObjectNode.class);
        jsonBody.set("attributes", attributesNode);

        if (log.isDebugEnabled()) {
            log.debug("Request Securosys TSB createKey: {}", jsonBody);
        }
        try {
            final String jsonStr = objectMapper.writeValueAsString(jsonBody);
            final HttpPost request = new HttpPost(tsbProperties.getTsbRestApi() + "/v1/key");
            request.addHeader("Content-Type", "application/json");
            request.setEntity(new StringEntity(jsonStr));

            final Map<String, Object> responseMap = doRequest(request, Constants.KEY_MANAGEMENT_TOKEN_NAME);
            final int statusCode = (int) responseMap.get("statusCode");
            final String responseBody = (String) responseMap.get("body");

            if (log.isDebugEnabled()) {
                log.debug("Response Securosys TSB createKey: {}", responseBody);
            }

            if (statusCode != HttpURLConnection.HTTP_OK && statusCode != HttpURLConnection.HTTP_CREATED) {
                handleErrorResponse(statusCode, responseBody);
            }

        }catch (IOException e){
            throw new BusinessException("Failed to create key.", BusinessReason.ERROR_GENERAL);
        }

    }

    private static Map<String, Boolean> prepareAttributes() {
        final Map<String, Boolean> attributes = new HashMap<>();
        attributes.put("decrypt", true);
        attributes.put("sign", true);
        attributes.put("wrap", false);
        attributes.put("unwrap", false);
        attributes.put("derive", false);
        attributes.put("extractable", false);
        attributes.put("modifiable", true);
        attributes.put("destroyable", true);
        attributes.put("sensitive", true);
        attributes.put("copyable", false);
        return attributes;
    }

    /**
     * Signs the given payload using the specified key and signature parameters.
     *
     * <p>This method sends a POST request to sign a payload using the TSB service. The request includes the payload,
     * key label, password, and signature algorithm. If the response indicates unauthorized access or another error,
     * appropriate exceptions are thrown. The generated signature is extracted from the response body.</p>
     *
     * @param label the label of the key to use for signing
     * @param password the password for the key, if applicable
     * @param payload the data to be signed
     * @param payloadType the type of payload being signed
     * @param signatureAlgorithm the algorithm used for signing
     * @return the generated signature as a string
     * For more information, @see <a href="https://docs.securosys.com/tsb/overview">
     */
    public String sign(String label, String password, String payload, String payloadType, String signatureType, String signatureAlgorithm, String metaData, String metaDataSignature) {
        final Map<String, Object> signRequest = new HashMap<>();

        signRequest.put("payload", payload);
        signRequest.put("payloadType", payloadType);
        if (label.isBlank()){
            createOrUpdateKey("Fireblocks_SigningKey", null, KEY_ALGORITHM, KEY_OID, 0);
            signRequest.put("signKeyName", "Fireblocks_SigningKey");
        }else {
            signRequest.put("signKeyName", label);
        }

        if (password != null) {
            signRequest.put("keyPassword", password);
        }
        signRequest.put("signatureAlgorithm", signatureAlgorithm);
        signRequest.put("signatureType", signatureType);

        if (metaData != null && !metaData.isEmpty()) {
            signRequest.put("metaData", metaData);
        }

        if (metaDataSignature != null && !metaDataSignature.isEmpty()) {
            signRequest.put("metaDataSignature", metaDataSignature);
        }

        final Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("signRequest", signRequest);

        if (log.isDebugEnabled()) {
            log.debug("Sign Request JSON: {}", requestBody);
        }

        try {
            final String jsonStr = objectMapper.writeValueAsString(requestBody);
            final HttpPost request = new HttpPost(tsbProperties.getTsbRestApi() + "/v1/sign");
            request.setHeader("Content-Type", "application/json");
            request.setEntity(new StringEntity(jsonStr));

            final Map<String, Object> responseMap = doRequest(request, Constants.KEY_OPERATION_TOKEN_NAME);
            final int statusCode = (int) responseMap.get("statusCode");
            final String responseBody = (String) responseMap.get("body");

            JsonNode responseData = objectMapper.readTree(responseBody);

            if (log.isDebugEnabled()) {
                log.debug("Response Securosys TSB sign: {}", responseBody);
            }

            if (statusCode != HttpURLConnection.HTTP_OK && statusCode != HttpURLConnection.HTTP_CREATED) {
                handleErrorResponse(statusCode, responseBody);
            }

            return responseData.get("signRequestId").asText();
        } catch (JsonProcessingException e) {
            throw new BusinessException("Failed to sign in TSB: " + e, BusinessReason.ERROR_GENERAL);
        }


    }

    public KeyAttributesDto getPublicKey(String label, String password){
        final ObjectNode jsonBody = objectMapper.createObjectNode();

        jsonBody.put("label", label);
        if (password != null && !password.isEmpty()){
            jsonBody.put("password", password);
        }

        if (log.isDebugEnabled()) {
            log.debug("Get key attributes request JSON: {}", jsonBody);
        }

        try {
            final String jsonStr = objectMapper.writeValueAsString(jsonBody);
            final HttpPost request = new HttpPost(tsbProperties.getTsbRestApi() + "/v1/key/attributes");
            request.setHeader("Content-Type", "application/json");
            request.setEntity(new StringEntity(jsonStr));

            final Map<String, Object> responseMap = doRequest(request, Constants.KEY_MANAGEMENT_TOKEN_NAME);
            final int statusCode = (int) responseMap.get("statusCode");
            final String responseBody = (String) responseMap.get("body");

            if (log.isDebugEnabled()) {
                log.debug("Response Securosys TSB get key attributes: {}", responseBody);
            }

            if (statusCode != HttpURLConnection.HTTP_OK) {
                handleErrorResponse(statusCode, responseBody);
            }

            JsonNode responseData = objectMapper.readTree(responseBody);
            JsonNode jsonData = responseData.get("json");

            KeyAttributesDto keyAttributes = new KeyAttributesDto();
            keyAttributes.setPublicKey(jsonData.get("publicKey").asText());
            keyAttributes.setAlgorithm(jsonData.get("algorithm").asText());

            JsonNode keySizeNode = jsonData.get("keySize");
            keyAttributes.setKeySize(keySizeNode != null && !keySizeNode.isNull() ? keySizeNode.asInt() : null);

            JsonNode curveOidNode = jsonData.get("curveOid");
            String curveOid = (curveOidNode != null && !curveOidNode.isNull())
                    ? curveOidNode.asText()
                    : null;

            if ("ED".equalsIgnoreCase(keyAttributes.getAlgorithm()) && curveOid == null) {
                JsonNode algorithmOidNode = jsonData.get("algorithmOid");
                if (algorithmOidNode != null && !algorithmOidNode.isNull()) {
                    curveOid = algorithmOidNode.asText();
                }
            }
            keyAttributes.setCurveOid(curveOid);

            return keyAttributes;

        } catch (Exception e) {
            throw new BusinessException("Failed to get key attributes.", BusinessReason.ERROR_GENERAL);
        }
    }

    /**
     * Returns the license from the TSB service.
     *
     * @return a {@link LicenseResponseDto} object containing the status of the request
     */
    public LicenseResponseDto getLicense() {

        final HttpGet request = new HttpGet(tsbProperties.getTsbRestApi() + "/v1/licenseInfo");
        request.addHeader("Content-Type", "application/json");

        try {
            final Map<String, Object> responseMap = doRequest(request, Constants.KEY_SERVICE_TOKEN_NAME);

            final int statusCode = (int) responseMap.get("statusCode");
            final String responseBody = (String) responseMap.get("body");

            if (log.isDebugEnabled()) {
                log.debug("Response Securosys TSB get license: {}", responseBody);
            }

            if (statusCode != HttpURLConnection.HTTP_OK) {
                handleErrorResponse(statusCode, responseBody);
            }

            LicenseResponseDto licenseResponseDto = objectMapper.readValue(responseBody, LicenseResponseDto.class);

            if (log.isDebugEnabled()) {
                log.debug("Status code get license is: {}", statusCode);
                log.debug("Response JSON: {}", licenseResponseDto);
            }
            return licenseResponseDto;
        } catch (IOException e) {
            throw new BusinessException("Failed to get request status", BusinessReason.ERROR_GENERAL);
        }
    }


    /**
     * Creates and attaches a self-signed certificate to the keypair.
     *
     * @param signKeyName the label of the key to use for signing
     * @param password the password for the key, if applicable
     * @param signatureAlgorithm the algorithm used for signing
     * For more information, @see <a href="https://docs.securosys.com/tsb/overview">
     */
    public void syncSelfSign(String signKeyName, String password, String signatureAlgorithm) {
        final Map<String, Object> requestBody = new HashMap<>();

        requestBody.put("signKeyName", signKeyName);
        requestBody.put("validity", VALIDITY);

        if (password != null) {
            requestBody.put("keyPassword", password);
        }
        requestBody.put("signatureAlgorithm", signatureAlgorithm);
        requestBody.put("certificateAuthority", true);

        final Map<String, Object> standardCertificateAttributes = new HashMap<>();
        standardCertificateAttributes.put("commonName", signKeyName);
        requestBody.put("standardCertificateAttributes", standardCertificateAttributes);

        requestBody.put("keyUsage", Collections.singletonList("KEY_CERT_SIGN"));
        requestBody.put("extendedKeyUsage", Collections.singletonList("ANY_EXTENDED_KEY_USAGE"));

        if (log.isDebugEnabled()) {
            log.debug("Self-sign Request JSON: {}", requestBody);
        }

        try {
            final String jsonStr = objectMapper.writeValueAsString(requestBody);
            final HttpPost request = new HttpPost(tsbProperties.getTsbRestApi() + "/v1/certificate/synchronous/selfsign");
            request.setHeader("Content-Type", "application/json");
            request.setEntity(new StringEntity(jsonStr));

            final Map<String, Object> responseMap = doRequest(request, Constants.KEY_MANAGEMENT_TOKEN_NAME);
            final int statusCode = (int) responseMap.get("statusCode");
            final String responseBody = (String) responseMap.get("body");

            if (log.isDebugEnabled()) {
                log.debug("Response Securosys TSB self-sign: {}", responseBody);
            }

            if (statusCode != HttpURLConnection.HTTP_OK && statusCode != HttpURLConnection.HTTP_CREATED) {
                handleErrorResponse(statusCode, responseBody);
            }

        } catch (Exception e) {
            throw new BusinessException("Failed to self-sign in TSB: " + e.getMessage(), BusinessReason.ERROR_GENERAL);
        }
    }

    public String generateCertificateRequest(String signKeyName, String password, String signatureAlgorithm) {

        // csrSignRequest
        final Map<String, Object> csrSignRequest = new HashMap<>();
        csrSignRequest.put("signKeyName", signKeyName);

        if (password != null) {
            csrSignRequest.put("keyPassword", password);
        }
        csrSignRequest.put("signatureAlgorithm", signatureAlgorithm);

        // standardCertificateAttributes
        final Map<String, Object> standardCertificateAttributes = new HashMap<>();
        standardCertificateAttributes.put("commonName", signKeyName);
        csrSignRequest.put("standardCertificateAttributes", standardCertificateAttributes);

        csrSignRequest.put("keyUsage", Collections.singletonList("DIGITAL_SIGNATURE"));
        csrSignRequest.put("extendedKeyUsage", Collections.singletonList("ANY_EXTENDED_KEY_USAGE"));

        final Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("csrSignRequest", csrSignRequest);


        if (log.isDebugEnabled()) {
            log.debug("Certificate Request JSON: {}", requestBody);
        }

        try {
            final String jsonStr = objectMapper.writeValueAsString(requestBody);
            final HttpPost request = new HttpPost(tsbProperties.getTsbRestApi() + "/v1/certificate/request");
            request.setHeader("Content-Type", "application/json");
            request.setEntity(new StringEntity(jsonStr, StandardCharsets.UTF_8));

            final Map<String, Object> responseMap = doRequest(request, Constants.KEY_MANAGEMENT_TOKEN_NAME);
            final int statusCode = (int) responseMap.get("statusCode");
            final String responseBody = (String) responseMap.get("body");

            JsonNode responseData = objectMapper.readTree(responseBody);

            if (log.isDebugEnabled()) {
                log.debug("Response Securosys TSB certificate request: {}", responseBody);
            }

            if (statusCode != HttpURLConnection.HTTP_OK && statusCode != HttpURLConnection.HTTP_CREATED) {
                handleErrorResponse(statusCode, responseBody);
            }

            return responseData.get("signRequestId").asText();

        } catch (Exception e) {
            throw new BusinessException("Failed to request certificate in TSB: " + e.getMessage(), BusinessReason.ERROR_GENERAL);
        }
    }

    public String generateSynchronousCertificateRequest(String signKeyName, String password, String signatureAlgorithm) {
        final Map<String, Object> requestBody = new HashMap<>();

        requestBody.put("signKeyName", signKeyName);

        if (password != null) {
            requestBody.put("keyPassword", password);
        }

        requestBody.put("signatureAlgorithm", signatureAlgorithm);

        // standardCertificateAttributes
        final Map<String, Object> standardCertificateAttributes = new HashMap<>();
        standardCertificateAttributes.put("commonName", signKeyName);
        requestBody.put("standardCertificateAttributes", standardCertificateAttributes);

        requestBody.put("keyUsage", Collections.singletonList("DIGITAL_SIGNATURE"));
        requestBody.put("extendedKeyUsage", Collections.singletonList("ANY_EXTENDED_KEY_USAGE"));

        if (log.isDebugEnabled()) {
            log.debug("Synchronous Certificate Request JSON: {}", requestBody);
        }

        try {
            final String jsonStr = objectMapper.writeValueAsString(requestBody);
            final HttpPost request = new HttpPost(tsbProperties.getTsbRestApi() + "/v1/certificate/synchronous/request");
            request.setHeader("Content-Type", "application/json");
            request.setEntity(new StringEntity(jsonStr, StandardCharsets.UTF_8));

            final Map<String, Object> responseMap = doRequest(request, Constants.KEY_MANAGEMENT_TOKEN_NAME);
            final int statusCode = (int) responseMap.get("statusCode");
            final String responseBody = (String) responseMap.get("body");

            JsonNode responseData = objectMapper.readTree(responseBody);

            if (log.isDebugEnabled()) {
                log.debug("Response Securosys TSB synchronous certificate request: {}", responseBody);
            }

            if (statusCode != HttpURLConnection.HTTP_OK && statusCode != HttpURLConnection.HTTP_CREATED) {
                handleErrorResponse(statusCode, responseBody);
            }

            return responseData.get("certificateSigningRequest").asText();

        } catch (Exception e) {
            throw new BusinessException("Failed to request synchronous certificate in TSB: " + e.getMessage(), BusinessReason.ERROR_GENERAL);
        }
    }


    public String signCertificate(String signKeyName, String password, String signatureAlgorithm, String certificateSigningRequest) {
        final Map<String, Object> requestBody = new HashMap<>();

        requestBody.put("signKeyName", signKeyName);

        if (password != null) {
            requestBody.put("keyPassword", password);
        }

        requestBody.put("signatureAlgorithm", signatureAlgorithm);

        final Map<String, Object> standardCertificateAttributes = new HashMap<>();
        standardCertificateAttributes.put("commonName", signKeyName);
        requestBody.put("standardCertificateAttributes", standardCertificateAttributes);

        requestBody.put("validity", VALIDITY);
        requestBody.put("certificateAuthority", false);
        requestBody.put("certificateSigningRequest", certificateSigningRequest);

        requestBody.put("keyUsage", Collections.singletonList("DIGITAL_SIGNATURE"));
        requestBody.put("extendedKeyUsage", Collections.singletonList("ANY_EXTENDED_KEY_USAGE"));

        if (log.isDebugEnabled()) {
            log.debug("Certificate Sign Request JSON: {}", requestBody);
        }

        try {
            final String jsonStr = objectMapper.writeValueAsString(requestBody);
            final HttpPost request = new HttpPost(tsbProperties.getTsbRestApi() + "/v1/certificate/synchronous/sign");
            request.setHeader("Content-Type", "application/json");
            request.setEntity(new StringEntity(jsonStr, StandardCharsets.UTF_8));

            final Map<String, Object> responseMap = doRequest(request, Constants.KEY_MANAGEMENT_TOKEN_NAME);
            final int statusCode = (int) responseMap.get("statusCode");
            final String responseBody = (String) responseMap.get("body");

            JsonNode responseData = objectMapper.readTree(responseBody);

            if (log.isDebugEnabled()) {
                log.debug("Response Securosys TSB certificate sign: {}", responseBody);
            }

            if (statusCode != HttpURLConnection.HTTP_OK && statusCode != HttpURLConnection.HTTP_CREATED) {
                handleErrorResponse(statusCode, responseBody);
            }

            if (responseData.has("certificate")) {
                return responseData.get("certificate").asText();
            } else {
                return responseData.toString();
            }

        } catch (Exception e) {
            throw new BusinessException("Failed to sign certificate in TSB: " + e.getMessage(), BusinessReason.ERROR_GENERAL);
        }
    }


    /**
     * Delete key from the HSM.
     *
     */
    public void deleteKey(String keyName) {

        final HttpDelete request = new HttpDelete(tsbProperties.getTsbRestApi() + "/v1/key/" + keyName);
        request.addHeader("Accept", "*/*");

        final Map<String, Object> responseMap = doRequest(request, Constants.KEY_MANAGEMENT_TOKEN_NAME);

        final int statusCode = (int) responseMap.get("statusCode");
        final String responseBody = (String) responseMap.get("body");

        if (log.isDebugEnabled()) {
            log.debug("Response Securosys TSB delete key: {}", responseBody);
        }
        if (statusCode != HttpURLConnection.HTTP_OK) {
            handleErrorResponse(statusCode, responseBody);
        }

    }

    private void handleErrorResponse(int statusCode, String responseBody) {

        JsonNode node = null;

        try {
            if (responseBody != null && !responseBody.isBlank()) {
                node = objectMapper.readTree(responseBody);
            }
        } catch (Exception ignored) {
            log.warn("Failed to parse TSB error response body");
        }

        String tsbMessage = node != null && node.has("message")
                ? node.get("message").asText()
                : "TSB returned HTTP " + statusCode;

        String tsbReason = node != null && node.has("reason")
                ? node.get("reason").asText()
                : null;

        BusinessReason reason = REASON_BY_NAME.get(tsbReason);
        if (reason != null) {
            throw new BusinessException(tsbMessage, reason);
        }

        switch (statusCode) {

            case 400:
                throw new BusinessException(
                        tsbMessage,
                        BusinessReason.ERROR_INPUT_VALIDATION_FAILED
                );

            case 401:
                throw new BusinessException(
                        tsbMessage,
                        BusinessReason.ERROR_INVALID_ACCESS_TOKEN
                );

            case 403:
                throw new BusinessException(
                        tsbMessage,
                        BusinessReason.ERROR_OPERATION_FORBIDDEN_FOR_KEY
                );

            case 404:
                throw new BusinessException(
                        tsbMessage,
                        BusinessReason.ERROR_RESOURCE_NOT_FOUND
                );

            default:
                if (statusCode >= 500) {
                    throw new BusinessException(
                            tsbMessage,
                            BusinessReason.ERROR_IN_SUBSYSTEM
                    );
                }

                throw new BusinessException(
                        tsbMessage,
                        BusinessReason.ERROR_GENERAL
                );
        }
    }
}
