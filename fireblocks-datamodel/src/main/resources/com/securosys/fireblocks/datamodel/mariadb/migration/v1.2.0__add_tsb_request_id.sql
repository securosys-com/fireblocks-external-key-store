alter table message_status add column tsb_request_id varchar(255) null;

update message_status set tsb_request_id = '';