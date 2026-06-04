package org.waterwood.waterfunadminservice.api.request.content;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;
import org.waterwood.waterfunservicecore.entity.Banner;
import org.waterwood.waterfunservicecore.entity.BannerPosition;
import org.waterwood.waterfunservicecore.entity.VisibleStatus;

import java.io.Serializable;
import java.time.Instant;

/**
 * Create Banner Request DTO for {@link Banner}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateBannerRequest implements Serializable {
    @Size(max = 64)
    @NotBlank
    private String title;
    @Size(max = 128)
    private String subtitle;
    @Size(max = 255)
    @URL
    private String linkUrl;
    private BannerPosition position;
    private Integer sortNo;
    private VisibleStatus status;
    private Instant startAt;
    private Instant endAt;
    @NotBlank
    private String imageUuid;
}

