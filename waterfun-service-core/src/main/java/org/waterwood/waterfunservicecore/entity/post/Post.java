package org.waterwood.waterfunservicecore.entity.post;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;
import org.waterwood.waterfunservicecore.entity.user.User;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Entity
@Table(name = "post")
public class Post {
    @Id
    @NotNull
    @Column(name = "id", nullable = false)
    private Long id;

    @Size(max = 32)
    @NotNull
    @Column(name = "title", nullable = false, length = 32)
    private String title;

    @Size(max = 64)
    @Column(name = "subtitle", length = 64)
    private String subtitle;

    @NotNull
    @Lob
    @Column(name = "content", nullable = false)
    private String content;

    @Size(max = 500)
    @Column(name = "summary", length = 500)
    private String summary;

    @Size(max = 255)
    @Column(name = "cover_img")
    private String coverImg;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @ColumnDefault("'0'")
    @Column(name = "view_count", columnDefinition = "int UNSIGNED")
    private Long viewCount;

    @ColumnDefault("'0'")
    @Column(name = "like_count", columnDefinition = "int UNSIGNED")
    private Long likeCount = 0L;

    @ColumnDefault("'0'")
    @Column(name = "comment_count", columnDefinition = "int UNSIGNED")
    private Long commentCount = 0L;

    @ColumnDefault("'0'")
    @Column(name = "collect_count", columnDefinition = "int UNSIGNED")
    private Long collectCount = 0L;

    @Size(max = 200)
    @Column(name = "slug", length = 200)
    private String slug;

    @Column(name = "published_at")
    private Instant publishedAt;

    @CreationTimestamp
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @UpdateTimestamp
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "post_tag",
            joinColumns = @JoinColumn(name = "post_id", foreignKey = @ForeignKey(name = "fk_post_tag_post")),
            inverseJoinColumns = @JoinColumn(name = "tag_id", foreignKey = @ForeignKey(name = "fk_post_tag_tag")),
            uniqueConstraints = @UniqueConstraint(columnNames = {"post_id", "tag_id"}, name = "uk_post_tag")
    )
    private List<Tag> tags = new ArrayList<>();

    @ColumnDefault("0")
    @Column(name = "is_deleted")
    private Boolean isDeleted = false;

    @ColumnDefault("'0'")
    @Column(name = "status", columnDefinition = "tinyint UNSIGNED")
    private PostStatus status = PostStatus.DRAFT;

    @ColumnDefault("'0'")
    @Column(name = "visibility", columnDefinition = "tinyint UNSIGNED")
    private PostVisibility visibility = PostVisibility.PUBLIC;

    @ColumnDefault("'0'")
    @Column(name = "edit_status", columnDefinition = "tinyint UNSIGNED")
    private PostEditStatus editStatus = PostEditStatus.NONE;

    @Size(max = 32)
    @Column(name = "edited_title", length = 32)
    private String editedTitle;

    @Size(max = 64)
    @Column(name = "edited_subtitle", length = 64)
    private String editedSubtitle;

    @Lob
    @Column(name = "edited_content")
    private String editedContent;

    @Size(max = 500)
    @Column(name = "edited_summary", length = 500)
    private String editedSummary;

    @Size(max = 255)
    @Column(name = "edited_cover_img")
    private String editedCoverImg;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "edited_category_id")
    private Category editedCategory;

    @Column(name = "edited_tag_ids")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<Integer> editedTagIds;

    @Column(name = "edited_new_tags")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> editedNewTags;

    @ColumnDefault("'1'")
    @Column(name = "version", columnDefinition = "int UNSIGNED")
    private Long version = 1L;

}