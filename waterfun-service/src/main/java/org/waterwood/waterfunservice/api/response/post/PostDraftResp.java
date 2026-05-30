package org.waterwood.waterfunservice.api.response.post;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.waterwood.api.VO.OptionVO;
import org.waterwood.waterfunservicecore.api.resp.CloudResPresignedUrlResp;
import org.waterwood.waterfunservicecore.entity.post.PostEditStatus;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PostDraftResp {
    private String editedTitle;
    private String editedContent;
    private String editedSummary;
    private CloudResPresignedUrlResp coverageImgPresignedUrl;
    private OptionVO<Long> editedCategoryId;
    private List<OptionVO<Long>> editedTagIds;
    private List<String> editedNewTagIds;
    private PostEditStatus editedStatus;
}
