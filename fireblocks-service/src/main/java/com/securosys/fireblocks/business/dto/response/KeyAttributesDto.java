package com.securosys.fireblocks.business.dto.response;

import lombok.Data;

@Data
public class KeyAttributesDto {

    private String publicKey;
    private String algorithm;
    private Integer keySize;
    private String curveOid;
}
