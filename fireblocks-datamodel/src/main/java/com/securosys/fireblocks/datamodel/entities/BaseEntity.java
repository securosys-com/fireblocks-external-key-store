// SPDX-FileCopyrightText: Copyright 2025 Securosys SA
// SPDX-License-Identifier: Apache-2.0

package com.securosys.fireblocks.datamodel.entities;

import java.util.Date;
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;

import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;


@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class BaseEntity {

	@CreatedDate
	@Column(name = "ctl_cre_ts", updatable = false)
	private Date ctlCreTs;

	@CreatedBy
	@Column(name = "ctl_cre_uid", updatable = false)
	@Size(max = 40)
	private String ctlCreUid;

	@LastModifiedDate
	@Column(name = "ctl_mod_ts")
	private Date ctlModTs;

	@LastModifiedBy
	@Column(name = "ctl_mod_uid")
	@Size(max = 40)
	private String ctlModUid;

	@Override
	public String toString() {
		return "BaseEntity{" +
				"ctlCreTs=" + ctlCreTs +
				", ctlCreUid='" + ctlCreUid + '\'' +
				", ctlModTs=" + ctlModTs +
				", ctlModUid='" + ctlModUid + '\'' +
				'}';
	}
}
