create table message_status (
    id varchar(36) not null,
    ctl_cre_ts datetime,
    ctl_cre_uid varchar(255),
    ctl_mod_ts datetime,
    ctl_mod_uid varchar(255),
    request_id varchar(36) not null,
    type varchar(255) not null,
    status varchar(255) not null,
    primary key (id),
    unique key uq_message_status_request_id (request_id)
) engine=InnoDB;

create table message_response (
    id varchar(36) not null,
    ctl_cre_ts datetime,
    ctl_cre_uid varchar(255),
    ctl_mod_ts datetime,
    ctl_mod_uid varchar(255),
    status_id varchar(36) not null,
    error_message longtext,
    primary key (id),
    unique key uq_message_response_status (status_id),
    constraint fk_message_response_status foreign key (status_id) references message_status (id) on delete cascade
) engine=InnoDB;

create table signed_message (
    id varchar(36) not null,
    ctl_cre_ts datetime,
    ctl_cre_uid varchar(255),
    ctl_mod_ts datetime,
    ctl_mod_uid varchar(255),
    response_id varchar(36) not null,
    message longtext not null,
    signature longtext not null,
    msg_index int not null,
    primary key (id),
    constraint fk_signed_message_response foreign key (response_id) references message_response (id) on delete cascade
) engine=InnoDB;

create table message_envelope (
    id varchar(36) not null,
    ctl_cre_ts datetime,
    ctl_cre_uid varchar(255),
    ctl_mod_ts datetime,
    ctl_mod_uid varchar(255),
    request_id varchar(36) not null,
    metadata_type varchar(255) not null,
    primary key (id),
    unique key uq_message_envelope_request_id (request_id)
) engine=InnoDB;

create table message (
    id varchar(36) not null,
    ctl_cre_ts datetime,
    ctl_cre_uid varchar(255),
    ctl_mod_ts datetime,
    ctl_mod_uid varchar(255),
    envelope_id varchar(36) not null,
    payload longtext not null,
    primary key (id),
    unique key uq_message_envelope (envelope_id),
    constraint fk_message_envelope foreign key (envelope_id) references message_envelope (id) on delete cascade
) engine=InnoDB;

create table payload_signature_data (
    id varchar(36) not null,
    ctl_cre_ts datetime,
    ctl_cre_uid varchar(255),
    ctl_mod_ts datetime,
    ctl_mod_uid varchar(255),
    message_id varchar(36) not null,
    signature varchar(255) not null,
    service varchar(255) not null,
    primary key (id),
    unique key uq_payload_signature_message (message_id),
    constraint fk_payload_signature_message foreign key (message_id) references message (id) on delete cascade
) engine=InnoDB;
