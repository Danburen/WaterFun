package org.waterwood.waterfunservice.api.response.post;

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
    private Long id;
    private String name;
    private String slug;
    private String description;
    private Long usageCount;
    private Instant createdAt;
    private Instant updateAt;
}