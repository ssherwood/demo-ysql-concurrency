create table if not exists job_status
(
    job_status_id  uuid primary key,
    job_status_txt varchar(50),
    job_notes_txt  varchar(100),
    job_end_ts     date
);