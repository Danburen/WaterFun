package org.waterwood.waterfunservicecore.entity.resource;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.Hibernate;
import org.waterwood.waterfunservicecore.entity.post.PostResourceId;

import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class AuditResourceId {
    @NotNull
    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Size(max = 36)
    @NotNull
    @Column(name = "resource_uuid", nullable = false, length = 36)
    private String resourceUuid;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        AuditResourceId entity = (AuditResourceId) o;
        return Objects.equals(this.taskId, entity.taskId) &&
                Objects.equals(this.resourceUuid, entity.resourceUuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(taskId, resourceUuid);
    }

}
