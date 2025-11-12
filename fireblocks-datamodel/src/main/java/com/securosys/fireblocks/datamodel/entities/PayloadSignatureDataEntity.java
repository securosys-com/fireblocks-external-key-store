// SPDX-FileCopyrightText: Copyright 2025 Securosys SA
// SPDX-License-Identifier: Apache-2.0

package com.securosys.fireblocks.datamodel.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "payload_signature_data")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayloadSignatureDataEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne
    @JoinColumn(name = "message_id", nullable = false)
    private MessageEntity message;

    @Lob
    @Column(name = "signature", nullable = false)
    private String signature;

    @Column(name = "service", nullable = false)
    private String service;
}
