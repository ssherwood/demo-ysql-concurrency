package io.undertree.demo.demoybdb.api;

import io.undertree.demo.demoybdb.domain.JobStatus;
import io.undertree.demo.demoybdb.domain.JobStatusService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/job-status")
public class JobStatusController {

    final JobStatusService jobStatusService;

    public JobStatusController(JobStatusService jobStatusService) {
        this.jobStatusService = jobStatusService;
    }

    @GetMapping
    public List<JobStatus> findAll() {
        return jobStatusService.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UUID create(@RequestBody JobStatus jobStatus) {
        return jobStatusService.create(jobStatus);
    }

    @PutMapping("/{id}/status/{status}")
    @ResponseStatus(HttpStatus.OK)
    public JobStatus updateStatus(@PathVariable UUID id, @PathVariable String status) {
        // transaction 1
        // uncomment one of these approaches...

        // using Spring Data JPA (w/ JPA transaction manager implied)
        // jobStatusService.updateStatusSpringDataJPA(id, status);

        // using Hibernate EntityManger (w/ JPA transaction manager specified)
        // jobStatusService.updateStatusHibernateEntityManager(id, status);

        // using JDBCTemplate manually (no JPA transaction manager)
        jobStatusService.updateStatusJDBC(id, status);

        // transaction 2
        return jobStatusService.findById(id);
    }
}
