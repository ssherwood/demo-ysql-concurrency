package io.undertree.demo.demoybdb.domain;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.util.Date;
import java.util.UUID;

@Entity
@Getter
@Setter
@ToString
public class JobStatus {
    @Id
    @GeneratedValue(generator = "system-uuid")
    private UUID jobStatusId;
    private String jobStatusTxt;
    private String jobNotesTxt;
    private Date jobEndTs;

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        return jobStatusId != null && jobStatusId.equals(((JobStatus) obj).jobStatusId);
    }
}
