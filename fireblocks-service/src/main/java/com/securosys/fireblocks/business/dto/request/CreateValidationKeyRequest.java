// SPDX-FileCopyrightText: Copyright 2025 Securosys SA
// SPDX-License-Identifier: Apache-2.0

package com.securosys.fireblocks.business.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Create validation key request.")
public class CreateValidationKeyRequest {

    @Schema(description = "The algorithm with which the key should be created. By default it will be RSA.", example="RSA", allowableValues = {"EC", "ED", "RSA"})
    private String algorithm;

    @Schema(description = "The oid of the curve used for the EC or ED algorithm. Mandatory if chosen algorithm is set to EC or ED. " +
            "secp224k1: 1.3.132.0.32\n" +
            "secp224r1: 1.3.132.0.33\n" +
            "secp256k1: 1.3.132.0.10\n" +
            "secp256r1 (also known as P-256 or prime256v1): 1.2.840.10045.3.1.7\n" +
            "secp384r1 (also known as P-384): 1.3.132.0.34\n" +
            "secp521r1 (also known as P-521): 1.3.132.0.35\n" +
            "x962p239v1: 1.2.840.10045.3.1.1\n" +
            "x962p239v2: 1.2.840.10045.3.1.2\n" +
            "x962p239v3: 1.2.840.10045.3.1.3\n" +
            "brainpool224r1: 1.3.36.3.3.2.8.1.1.1\n" +
            "brainpool256r1: 1.3.36.3.3.2.8.1.1.7\n" +
            "brainpool320r1: 1.3.36.3.3.2.8.1.1.9\n" +
            "brainpool384r1: 1.3.36.3.3.2.8.1.1.11\n" +
            "brainpool512r1: 1.3.36.3.3.2.8.1.1.13\n" +
            "frp256v1: 1.2.250.1.223.101.256.1\n" +
            "Ed25519: 1.3.101.112")
    private String curveOid;

    @Schema(description = "The length of the key. Only applicable for RSA. By default it will be 2048.", example = "2048")
    private Integer keySize;
}
