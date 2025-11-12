create table message_status (
    id uuid primary key,
    ctl_cre_ts timestamp,
    ctl_cre_uid varchar(255),
    ctl_mod_ts timestamp,
    ctl_mod_uid varchar(255),
    request_id uuid not null unique,
    type varchar(255) not null,
    status varchar(255) not null,
    tsb_request_id varchar(255) not null
);

create table message_response (
    id uuid primary key,
    ctl_cre_ts timestamp,
    ctl_cre_uid varchar(255),
    ctl_mod_ts timestamp,
    ctl_mod_uid varchar(255),
    status_id uuid not null unique,
    constraint fk_message_response_status foreign key (status_id) references message_status (id) on delete cascade
);

create table signed_message (
    id uuid primary key,
    ctl_cre_ts timestamp,
    ctl_cre_uid varchar(255),
    ctl_mod_ts timestamp,
    ctl_mod_uid varchar(255),
    response_id uuid not null,
    message text not null,
    signature text not null,
    msg_index int not null,
    constraint fk_signed_message_response foreign key (response_id) references message_response (id) on delete cascade
);

create table message_envelope (
    id uuid primary key,
    ctl_cre_ts timestamp,
    ctl_cre_uid varchar(255),
    ctl_mod_ts timestamp,
    ctl_mod_uid varchar(255),
    request_id uuid not null unique,
    metadata_type varchar(255) not null
);

create table message (
    id uuid primary key,
    ctl_cre_ts timestamp,
    ctl_cre_uid varchar(255),
    ctl_mod_ts timestamp,
    ctl_mod_uid varchar(255),
    envelope_id uuid not null unique,
    payload text not null,
    constraint fk_message_envelope foreign key (envelope_id) references message_envelope (id) on delete cascade
);

create table payload_signature_data (
    id uuid primary key,
    ctl_cre_ts timestamp,
    ctl_cre_uid varchar(255),
    ctl_mod_ts timestamp,
    ctl_mod_uid varchar(255),
    message_id uuid not null unique,
    signature text not null,
    service varchar(255) not null,
    constraint fk_payload_signature_message foreign key (message_id) references message (id) on delete cascade
);
