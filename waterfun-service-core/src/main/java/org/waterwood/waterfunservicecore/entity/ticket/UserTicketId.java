package org.waterwood.waterfunservicecore.entity.ticket;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.Hibernate;

import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
@Embeddable
public class UserTicketId implements Serializable {
    private static final long serialVersionUID = -6937155365914541239L;
    @NotNull
    @Column(name = "user_uid", nullable = false)
    private Long userUid;

    @NotNull
    @Column(name = "ticket_id", nullable = false)
    private Long ticketId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        UserTicketId entity = (UserTicketId) o;
        return Objects.equals(this.ticketId, entity.ticketId) &&
                Objects.equals(this.userUid, entity.userUid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ticketId, userUid);
    }

}