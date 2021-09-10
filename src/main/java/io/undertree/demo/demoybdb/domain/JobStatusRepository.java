package io.undertree.demo.demoybdb.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JobStatusRepository extends JpaRepository<JobStatus, UUID> {
}
