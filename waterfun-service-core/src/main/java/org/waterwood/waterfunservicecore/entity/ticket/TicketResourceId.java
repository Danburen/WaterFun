package org.waterwood.waterfunservicecore.entity.ticket;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.Hibernate;

import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
@Embeddable
public class TicketResourceId implements Serializable {
    private static final long serialVersionUID = 3257424915351880507L;
    @NotNull
    @Column(name = "ticket_id", nullable = false)
    private Long ticketId;

    @Size(max = 36)
    @NotNull
    @Column(name = "resource_uuid", nullable = false, length = 36)
    private String resourceUuid;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        TicketResourceId entity = (TicketResourceId) o;
        return Objects.equals(this.ticketId, entity.ticketId) &&
                Objects.equals(this.resourceUuid, entity.resourceUuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ticketId, resourceUuid);
    }

}