package io.undertree.demo.demoybdb.domain;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class JobStatusService {
    final JdbcTemplate jdbcTemplate;
    final EntityManager entityManager;
    final JobStatusRepository jobStatusRepository;

    public JobStatusService(JdbcTemplate jdbcTemplate, EntityManager entityManager, JobStatusRepository jobStatusRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.entityManager = entityManager;
        this.jobStatusRepository = jobStatusRepository;
    }

    public List<JobStatus> findAll() {
        return jobStatusRepository.findAll();
    }

    //@Transactional(readOnly = true)
    public JobStatus findById(UUID id) {
        return jobStatusRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException(String.format("%s not found", id)));
    }

    public UUID create(JobStatus jobStatus) {
        return jobStatusRepository.saveAndFlush(jobStatus).getJobStatusId();
    }

    /**
     * @param id
     * @param status
     * @return
     */
    @Retryable(value = SQLException.class, exceptionExpression = "message.contains('could not execute statement')", maxAttempts = 3)
    // @Transactional is implied
    public JobStatus updateStatusSpringDataJPA(UUID id, String status) {
        var jobStatus = jobStatusRepository.findById(id).orElseThrow(IllegalStateException::new);

        var currentStatus = jobStatus.getJobStatusTxt();
        // do state change rules here...

        jobStatus.setJobStatusTxt(status);

        return jobStatusRepository.saveAndFlush(jobStatus); // this forces a save of the entire object
    }

    /**
     * Using Spring Retry, will attempt to rerun the transaction if
     * org.hibernate.exception.LockAcquisitionException: could not execute statement
     *
     * @param id
     * @param status
     * @return
     */
    @Retryable(value = SQLException.class, exceptionExpression = "message.contains('could not execute statement')", maxAttempts = 3)
    @Transactional
    //@Transactional(isolation = Isolation.SERIALIZABLE)
    public JobStatus updateStatusHibernateEntityManager(UUID id, String status) {

        // do state change rules here...
        var jobStatus = entityManager.find(JobStatus.class, id);
        var currentStatus = jobStatus.getJobStatusTxt();

        if (!stateChangeAllowed(currentStatus, status)) {
            // this is a way to work around Hibernate touching all the columns via save
            var updateStmt = entityManager.createQuery("UPDATE JobStatus set jobStatusTxt = :jobStatusTxt where jobStatusId = :jobStatusId");
            updateStmt.setParameter("jobStatusTxt", status);
            updateStmt.setParameter("jobStatusId", jobStatus.getJobStatusId());
            updateStmt.executeUpdate();
        } else {
            throw new IllegalStateException(String.format("Changing status from %s to %s is not allowed", currentStatus, status));
        }

        return entityManager.find(JobStatus.class, id); // we get the previous value, not the update?
    }

    /**
     * This scenario demonstrates using the JDBCTemplate and directly getting a DataSource connection; this uses the
     * lower level JDBC database semantics, but also how to operate directly to the database WITHOUT an implied
     * JPA Transaction Manager
     *
     * @param id
     * @param status
     * @return
     */
    public JobStatus updateStatusJDBC(UUID id, String status) {
        JobStatus jobStatus = null;

        try (var connection = jdbcTemplate.getDataSource().getConnection()) {

            // do a normal select
            try (var preparedStatement = connection.prepareStatement("select * from job_status where job_status_id = ?")) {
                preparedStatement.setObject(1, id);
                preparedStatement.execute();

                try (var resultSet = preparedStatement.getResultSet()) {
                    List<JobStatus> jobStatuses = new ArrayList<>();

                    while (resultSet.next()) {
                        var jobStatusTemp = new JobStatus();
                        jobStatusTemp.setJobStatusId((UUID) resultSet.getObject("job_status_id"));
                        jobStatusTemp.setJobStatusTxt(resultSet.getString("job_status_txt"));
                        jobStatusTemp.setJobNotesTxt(resultSet.getString("job_notes_txt"));
                        jobStatusTemp.setJobEndTs(resultSet.getDate("job_end_ts"));
                        jobStatuses.add(jobStatusTemp);
                    }

                    jobStatus = jobStatuses.get(0);
                }
            }

            // do some business logic here...

            // now do an update...
            try (var preparedStatement = connection.prepareStatement("update job_status set job_status_txt = ? where job_status_id = ?")) {
                preparedStatement.setString(1, status);
                preparedStatement.setObject(2, id);
                preparedStatement.executeUpdate();
            }

            // autocommit is enabled...
        } catch (SQLException sqlException) {
            throw new RuntimeException("Failed to update status", sqlException);
        }

        return jobStatus;
    }

    private static final class JobStatusMapper implements RowMapper<JobStatus> {
        public JobStatus mapRow(ResultSet rs, int rowNum) throws SQLException {
            JobStatus jobStatus = new JobStatus();
            jobStatus.setJobStatusId(UUID.fromString(rs.getString("job_status_id")));
            jobStatus.setJobStatusTxt(rs.getString("job_status_txt"));
            return jobStatus;
        }
    }

    private boolean stateChangeAllowed(String currentState, String nextState) {
        if (currentState == null || currentState.isEmpty()) {
            return "INITIAL".equals(nextState);
        } else if ("INITIAL".equals(currentState)) {
            return "PENDING".equals(nextState) || "CLOSED".equals(nextState);
        } else if ("PENDING".equals(currentState)) {
            return "OPEN".equals(nextState);
        } else if ("OPEN".equals(currentState)) {
            return "CLOSED".equals(nextState) || "HOLD".equals(nextState);
        } else if ("HOLD".equals(currentState)) {
            return "OPEN".equals(nextState);
        }

        return false;
    }

}
