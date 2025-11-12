// SPDX-FileCopyrightText: Copyright 2025 Securosys SA
// SPDX-License-Identifier: Apache-2.0

package com.securosys.fireblocks.datamodel.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "signed_message")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignedMessageEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "response_id", nullable = false)
    private MessageResponseEntity response;

    @Column(name = "message", nullable = false)
    private String message;

    @Column(name = "signature", nullable = false)
    private String signature;

    @Column(name = "msg_index", nullable = false)
    private int index;
}
