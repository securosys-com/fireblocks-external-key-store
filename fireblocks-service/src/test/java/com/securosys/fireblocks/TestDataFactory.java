// SPDX-FileCopyrightText: Copyright 2025 Securosys SA
// SPDX-License-Identifier: Apache-2.0

package com.securosys.fireblocks;

import com.securosys.fireblocks.business.dto.customServer.*;
import com.securosys.fireblocks.datamodel.entities.RequestType;

import java.util.List;
import java.util.UUID;

public class TestDataFactory {

    public static MessagesRequest buildMessagesRequest(String signingDeviceKeyId, String serviceName, String signatureVariant, String algorithm) {

        Message message = new Message(
                new PayloadSignatureData(resolveSignature(signatureVariant), serviceName),
                buildPayload(signingDeviceKeyId, algorithm)
        );

        TransportMetadata metadata = new TransportMetadata(
                UUID.randomUUID(),
                RequestType.KEY_LINK_TX_SIGN_REQUEST
        );

        MessageEnvelope envelope = new MessageEnvelope(message, metadata);
        return new MessagesRequest(List.of(envelope));
    }

    public static MessagesRequest buildMessagesInOneEnvelopeRequest(String signingDeviceKeyId, String serviceName, String signatureVariant, String algorithm, boolean valid) {

        String payload;
        if (valid){
            payload = buildMultipleMessagesPayload(signingDeviceKeyId, algorithm);
        } else {
            payload = buildWrongMultipleMessagesPayload(signingDeviceKeyId, algorithm);
        }
        Message message = new Message(
                new PayloadSignatureData(resolveSignature(signatureVariant), serviceName),
                payload
        );

        TransportMetadata metadata = new TransportMetadata(
                UUID.randomUUID(),
                RequestType.KEY_LINK_TX_SIGN_REQUEST
        );

        MessageEnvelope envelope = new MessageEnvelope(message, metadata);
        return new MessagesRequest(List.of(envelope));
    }

    public static MessagesRequest buildMultipleMessagesRequest(String signingDeviceKeyId, String serviceName, String signatureVariant, String algorithm, boolean valid) {

        MessageEnvelope envelope1 = new MessageEnvelope(
                new Message(new PayloadSignatureData(resolveSignature(signatureVariant), serviceName),
                        buildPayload(signingDeviceKeyId, algorithm)),
                new TransportMetadata(UUID.randomUUID(), RequestType.KEY_LINK_TX_SIGN_REQUEST)
        );

        String payload;
        if (valid){
            payload = buildPayload(signingDeviceKeyId, algorithm);
        } else {
            payload = buildPayload("wrongKey", algorithm);
        }
        MessageEnvelope envelope2 = new MessageEnvelope(
                new Message(new PayloadSignatureData(resolveSignature(signatureVariant), serviceName),
                        payload),
                new TransportMetadata(UUID.randomUUID(), RequestType.KEY_LINK_TX_SIGN_REQUEST)
        );

        return new MessagesRequest(List.of(envelope1, envelope2));
    }

    private static String resolveSignature(String variant) {
        return switch (variant) {
            case "VALID_SIGNATURE" -> "415c158563f4c32c3520c451ec1c50b2a7002719dbc8c344819bf64dd0dc9eb284594b2eb57237aafc2940e65f2b6b48eaba7e5d84e54c6146dfdac1ccb2e6591e48edc740247d121c6e37f429c89b592d1a3339a2e5d7650939b40e2f789eb9463c70c2894f403c534715f154738047a29c3f728cccd95043122573d39ff4ab4104f6e5cfac74f73632652635c4d3c6925972612adf830d42c8df046cfeb1b96f8955b9f3c67b61e46e7d03c79e52ac2c54a6eca376a15e34190a58047090e44ed0afdac9833d69af417402a8d9f506c496c01e9927a119b8f2050fed9dbf82bda00de60be77b0d132902458b1c407691e601b4df740f3e0138e5c2f2bb31be";
            case "INVALID_SIGNATURE" -> "WrongSignature";
            default -> variant;
        };
    }

    private static String buildPayload(String signingDeviceKeyId, String algorithm) {
        return """
                {
                  "tenantId": "0a4cc5e4-182d-5c9c-b771-c8bc8636733c",
                  "txId": "955a13d1-9e56-403e-91fd-6d28918641e5",
                  "keyId": "6ea827ed-cb5e-4e17-906c-96cc1c0f4e8c",
                  "userAccessToken": "dummy-access-token",
                  "signingDeviceKeyId": "%s",
                  "algorithm": "%s",
                  "type": "KEY_LINK_TX_SIGN_REQUEST",
                  "messagesToSign": [
                    {
                      "message": "32168959a247dddd04ece6f6a646a689c9548b6bbdb32e8567343bfcfaa5bda1",
                      "index": 0
                    }
                  ],
                  "metadata": {
                    "source": "unit-test",
                    "note": "test payload for signature verification"
                  }
                }
                """.formatted(signingDeviceKeyId, algorithm);
    }

    private static String buildMultipleMessagesPayload(String signingDeviceKeyId, String algorithm) {
        return """
                {
                  "tenantId": "0a4cc5e4-182d-5c9c-b771-c8bc8636733c",
                  "txId": "955a13d1-9e56-403e-91fd-6d28918641e5",
                  "keyId": "6ea827ed-cb5e-4e17-906c-96cc1c0f4e8c",
                  "userAccessToken": "dummy-access-token",
                  "signingDeviceKeyId": "%s",
                  "algorithm": "%s",
                  "type": "KEY_LINK_TX_SIGN_REQUEST",
                  "messagesToSign": [
                    {
                      "message": "32168959a247dddd04ece6f6a646a689c9548b6bbdb32e8567343bfcfaa5bda1",
                      "index": 0
                    },
                    {
                      "message": "32168959a247dddd04ece6f6a646a689c9548b6bbdb32e8567343bfcfaa5bda1",
                      "index": 1
                    }
                  ],
                  "metadata": {
                    "source": "unit-test",
                    "note": "test payload for signature verification"
                  }
                }
                """.formatted(signingDeviceKeyId, algorithm);
    }

    private static String buildWrongMultipleMessagesPayload(String signingDeviceKeyId, String algorithm) {
        return """
                {
                  "tenantId": "0a4cc5e4-182d-5c9c-b771-c8bc8636733c",
                  "txId": "955a13d1-9e56-403e-91fd-6d28918641e5",
                  "keyId": "6ea827ed-cb5e-4e17-906c-96cc1c0f4e8c",
                  "userAccessToken": "dummy-access-token",
                  "signingDeviceKeyId": "%s",
                  "algorithm": "%s",
                  "type": "KEY_LINK_TX_SIGN_REQUEST",
                  "messagesToSign": [
                  {
                      "message": "dGVzdFdyb25nTWVzc2FnZQ==",
                      "index": 0
                    },
                    {
                      "message": "32168959a247dddd04ece6f6a646a689c9548b6bbdb32e8567343bfcfaa5bda1",
                      "index": 1
                    }
                  ],
                  "metadata": {
                    "source": "unit-test",
                    "note": "test payload for signature verification"
                  }
                }
                """.formatted(signingDeviceKeyId, algorithm);
    }
}
