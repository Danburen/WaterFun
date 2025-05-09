package org.waterwood.waterfunservice.entity.User;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

@Getter
@Setter
@Entity
@Table(name = "user_data_archive", schema = "waterfun")
public class UserDataArchive {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "table_name", nullable = false, length = 50)
    private String tableName;

    @Column(name = "original_data", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> originalData;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "archived_at")
    private Instant archivedAt;

    @Column(name = "archived_by", nullable = false, length = 50)
    private String archivedBy;

    @Column(name = "reason", nullable = false)
    private String reason;

}