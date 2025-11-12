// SPDX-FileCopyrightText: Copyright 2025 Securosys SA
// SPDX-License-Identifier: Apache-2.0

package com.securosys.fireblocks.datamodel.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "message_envelope")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageEnvelopeEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "request_id", nullable = false, unique = true)
    private UUID requestId;

    @OneToOne(mappedBy = "envelope", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private MessageEntity message;

    @Enumerated(EnumType.STRING)
    @Column(name = "metadata_type", nullable = false)
    private RequestType metadataType;
}
