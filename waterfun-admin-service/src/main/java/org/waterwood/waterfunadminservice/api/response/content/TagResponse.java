package org.waterwood.waterfunadminservice.api.response.content;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.waterwood.waterfunservicecore.entity.post.Tag;

import java.io.Serializable;
import java.time.Instant;

/**
 * DTO for {@link Tag}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TagResponse implements Serializable {
    private Integer id;
    private String name;
    private String slug;
    private String description;
    private Long usageCount;
    private Long creatorId;
    private Instant createdAt;
    private Instant updateAt;
}