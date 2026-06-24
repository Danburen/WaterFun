package org.waterwood.waterfunservice.service.report;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.waterwood.waterfunservice.api.UserUploadContext;
import org.waterwood.waterfunservice.api.UserUploadPolicyReq;
import org.waterwood.waterfunservice.api.response.ticket.TicketStatsResponse;
import org.waterwood.waterfunservice.api.response.ticket.UserTicketDetailResponse;
import org.waterwood.waterfunservice.api.response.ticket.UserTicketListResponse;
import org.waterwood.waterfunservicecore.api.req.CloudPutCallbackReq;
import org.waterwood.waterfunservicecore.api.resp.PresignedResp;
import org.waterwood.waterfunservicecore.entity.audit.AuditType;
import org.waterwood.waterfunservicecore.entity.audit.TargetType;
import org.waterwood.waterfunservicecore.entity.security.PenaltyType;
import org.waterwood.waterfunservicecore.entity.ticket.TicketAuditStatus;
import org.waterwood.waterfunservicecore.entity.ticket.TicketType;

import java.util.List;

public interface ReportService {

    Long submitReport(String targetId, TargetType targetType, AuditType type, String reason, List<String> resourceUuids);
    Long submitSuggestion(String content, List<String> resourceUuids);
    Long submitFeedback(String content, List<String> resourceUuids);
    Long submitAppeal(String targetId, TargetType targetType, String content, PenaltyType penaltyType, List<String> resourceUuids);
    void cancelReport(Long userUid, Long reportId);

    /**
     * Handle report upload attachment image
     * the image default associated to the user
     * @param request {@link UserUploadPolicyReq} the user upload policy request
     * @return {@link List<PresignedResp>} the list of presigned response for the upload request
     */
    List<PresignedResp> handleImageUpload(UserUploadPolicyReq request);

    /**
     * Handle user upload image attachment callback
     *
     * @param request {@link  CloudPutCallbackReq}
     * @param context {@link  UserUploadContext<Long>} upload context
     */
    void handleImageCallback(CloudPutCallbackReq request, UserUploadContext<Long> context);

    Page<UserTicketListResponse> listUserTickets(Long userUid, TicketType ticketType, TicketAuditStatus status, Pageable pageable);
    UserTicketDetailResponse getUserTicketDetail(Long userUid, Long ticketId);
    TicketStatsResponse getUserTicketStats(Long userUid);
}
