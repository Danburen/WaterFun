package org.waterwood.waterfunservicecore.entity.post;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.waterwood.waterfunservicecore.entity.user.User;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "comment")
public class Comment {
    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Comment parent;

    @Column(name = "post_id", insertable = false, updatable = false)
    private Long postId;
    @Column(name = "parent_id", insertable = false, updatable = false)
    private Long parentId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_uid", nullable = false)
    private User author;

    @NotNull
    @Lob
    @Column(name = "content", nullable = false)
    private String content;

    @ColumnDefault("'1'")
    @Column(name = "status", columnDefinition = "tinyint UNSIGNED")
    private CommentStatus status = CommentStatus.NORMAL;

    @ColumnDefault("'0'")
    @Column(name = "like_count", columnDefinition = "int UNSIGNED")
    private Long likeCount = 0L;

    @ColumnDefault("'0'")
    @Column(name = "reply_count", columnDefinition = "int UNSIGNED")
    private Long replyCount = 0L;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP(3)")
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP(3)")
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "root_id")
    private Comment root;

    @ColumnDefault("0")
    @Column(name = "is_pined")
    private Boolean isPined = false;

    public boolean isDeleted() {
        return status == CommentStatus.DELETED;
    }

    public boolean isTopLevel() {
        return parent == null;
    }
}