// SPDX-FileCopyrightText: Copyright 2025 Securosys SA
// SPDX-License-Identifier: Apache-2.0

package com.securosys.fireblocks.datamodel.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "message_status")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MessageStatusEntity extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID id;

    @Column(name = "request_id", nullable = false, unique = true)
    private UUID requestId;

    @Column(name = "tsb_request_id")
    private String tsbRequestId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private ResponseType type;

    @Column(name = "status", nullable = false)
    private String status;

    @OneToOne(mappedBy = "status", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private MessageResponseEntity response;
}
