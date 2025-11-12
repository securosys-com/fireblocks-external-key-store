// SPDX-FileCopyrightText: Copyright 2025 Securosys SA
// SPDX-License-Identifier: Apache-2.0

package com.securosys.fireblocks.business.dto.customServer;


import com.securosys.fireblocks.business.exceptions.BusinessException;
import com.securosys.fireblocks.business.exceptions.BusinessReason;
import lombok.Getter;

@Getter
public enum Algorithm {

    NONE_WITH_ECDSA("NONEwithECDSA"),
    SHA1_WITH_ECDSA("SHA1withECDSA"),
    SHA224_WITH_ECDSA("SHA224withECDSA"),
    SHA256_WITH_ECDSA("SHA256withECDSA"),
    DOUBLE_SHA256_WITH_ECDSA("DOUBLE_SHA256_WITH_ECDSA"),
    SHA384_WITH_ECDSA("SHA384withECDSA"),
    SHA512_WITH_ECDSA("SHA512withECDSA"),
    SHA3224_WITH_ECDSA("SHA3224withECDSA"),
    SHA3256_WITH_ECDSA("SHA3256withECDSA"),
    SHA3384_WITH_ECDSA("SHA3384withECDSA"),
    SHA3512_WITH_ECDSA("SHA3512withECDSA"),
    EDDSA("EdDSA"),
    KECCAK224_WITH_ECDSA("KECCAK224withECDSA"),
    KECCAK256_WITH_ECDSA("KECCAK256withECDSA"),
    KECCAK384_WITH_ECDSA("KECCAK384withECDSA"),
    KECCAK512_WITH_ECDSA("KECCAK512withECDSA"),
    ECDSA_SECP256K1("SHA256withECDSA"),
    EDDSA_ED25519("EdDSA");

    private String algorithm;

    Algorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public static Algorithm fromAlgorithm(String algorithm) {
        for (Algorithm signatureAlgorithm : values()) {
            if (signatureAlgorithm.getAlgorithm().equalsIgnoreCase(algorithm)) {
                return signatureAlgorithm;
            }
        }
        String msg = String.format("algorithm='%s' can not be mapped to SignatureAlgorithm", algorithm);
        throw new BusinessException(msg, BusinessReason.ERROR_INVALID_VALUE_FOR_ENUM);
    }
}
