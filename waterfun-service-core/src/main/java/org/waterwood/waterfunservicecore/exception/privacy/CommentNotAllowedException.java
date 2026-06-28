package org.waterwood.waterfunservicecore.exception.privacy;

import org.waterwood.api.BaseResponseCode;

/**
 * Thrown when a user tries to comment on a post but the post author's privacy settings
 * (commentPermission) prevent it.
 */
public class CommentNotAllowedException extends UserPrivacyBlockException {
    public CommentNotAllowedException() {
        super(BaseResponseCode.PRIVACY_COMMENT_NOT_ALLOWED);
    }
}
