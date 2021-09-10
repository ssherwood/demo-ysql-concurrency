# Demo Spring Boot Application using Yugabyte

## Overview

The purpose of this project is to highlight concurrent transaction modification exception behavior in Yugabyte
at various isolation levels and transaction configurations.  The application simulates a simple job status tracking
system but focuses on highly concurrent modifications to specific rows (as modeled in the accompanying jmeter test
suite).

## Transactional Errors

```
CannotSerializeTransactionException
org.postgresql.util.PSQLException: ERROR: Query error: Restart read required at:
```

```sql
update job_status set job_status_txt=?, job_notes_txt=?, job_end_ts=? where job_status_id=?
```

```sql
update job_status set job_status_txt=?, job_notes_txt=?, job_end_ts=? where job_status_id=? and job_status_txt='INITIAL'
```

## Observations

### Using JDBCTemplate Manually

```
│2021-09-10 13:47:42.444 UTC [725601] LOG:  execute <unnamed>: select * from job_status where job_status_id = $1                                         │
│2021-09-10 13:47:42.444 UTC [725601] DETAIL:  parameters: $1 = '91d59773-6c07-47e1-9135-ef7144d7d7e6'                                                   │
│2021-09-10 13:47:42.446 UTC [725601] LOG:  execute <unnamed>: update job_status set job_status_txt = $1 where job_status_id = $2                        │
│2021-09-10 13:47:42.446 UTC [725601] DETAIL:  parameters: $1 = 'TEST12', $2 = '91d59773-6c07-47e1-9135-ef7144d7d7e6'                                    │
│2021-09-10 13:47:42.475 UTC [725601] LOG:  execute <unnamed>: BEGIN READ ONLY                                                                           │
│2021-09-10 13:47:42.477 UTC [725601] LOG:  execute <unnamed>: select jobstatus0_.job_status_id as job_stat1_0_0_, jobstatus0_.job_end_ts as job_end_2_0_│
│2021-09-10 13:47:42.477 UTC [725601] DETAIL:  parameters: $1 = '91d59773-6c07-47e1-9135-ef7144d7d7e6'                                                   │
│2021-09-10 13:47:42.494 UTC [725601] LOG:  execute S_1: COMMIT
```

### Using Hibernate EntityManager manually

```
│2021-09-10 14:03:44.397 UTC [728759] LOG:  execute <unnamed>: BEGIN                                                                                     │
│2021-09-10 14:03:44.409 UTC [728759] LOG:  execute <unnamed>: select jobstatus0_.job_status_id as job_stat1_0_0_, jobstatus0_.job_end_ts as job_end_2_0_│
│2021-09-10 14:03:44.409 UTC [728759] DETAIL:  parameters: $1 = '91d59773-6c07-47e1-9135-ef7144d7d7e6'                                                   │
│2021-09-10 14:03:44.464 UTC [728759] LOG:  execute <unnamed>: update job_status set job_status_txt=$1 where job_status_id=$2                            │
│2021-09-10 14:03:44.464 UTC [728759] DETAIL:  parameters: $1 = 'TEST13', $2 = '91d59773-6c07-47e1-9135-ef7144d7d7e6'                                    │
│I0910 14:03:44.465154 728759 pg_txn_manager.cc:250] Using TServer host_port: 127.0.0.1:9100                                                             │
│I0910 14:03:44.475868 728759 thread_pool.cc:171] Starting thread pool { name: TransactionManager queue_limit: 500 max_workers: 50 }                     │
│2021-09-10 14:03:44.487 UTC [728759] LOG:  execute S_1: COMMIT                                                                                          │
│2021-09-10 14:03:44.499 UTC [728759] LOG:  execute <unnamed>: BEGIN READ ONLY                                                                           │
│2021-09-10 14:03:44.499 UTC [728759] LOG:  execute <unnamed>: select jobstatus0_.job_status_id as job_stat1_0_0_, jobstatus0_.job_end_ts as job_end_2_0_│
│2021-09-10 14:03:44.499 UTC [728759] DETAIL:  parameters: $1 = '91d59773-6c07-47e1-9135-ef7144d7d7e6'                                                   │
│2021-09-10 14:03:44.504 UTC [728759] LOG:  execute S_1: COMMIT
```