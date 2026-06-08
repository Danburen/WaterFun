package org.waterwood.waterfunservicecore.entity.post;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PostAuthorUidTitleDO {
    private Long authorUid;
    private String title;
    private Long coverageResourceUuid;
}
