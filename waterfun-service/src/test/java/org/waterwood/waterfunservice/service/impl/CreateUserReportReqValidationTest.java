package org.waterwood.waterfunservice.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.waterwood.waterfunservice.api.request.CreateUserReportReq;
import org.waterwood.waterfunservicecore.entity.audit.AuditType;
import org.waterwood.waterfunservicecore.entity.audit.TargetType;
import org.waterwood.waterfunservicecore.entity.ticket.TicketType;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("CreateUserReportReq DTO 校验逻辑测试")
public class CreateUserReportReqValidationTest {

    @Test
    @DisplayName("CONTENT_REPORT + AuditType.OTHER 必须有 reason")
    void contentReport_otherType_requiresReason() {
        CreateUserReportReq req = new CreateUserReportReq();
        req.setTicketType(TicketType.CONTENT_REPORT);
        req.setType(AuditType.OTHER);
        req.setTargetId("123");
        req.setTargetType(TargetType.POST);
        // AuditType.OTHER + no reason → invalid
        assertFalse(req.isReasonValid(),
                "CONTENT_REPORT + OTHER type 无 reason 应不通过校验");
    }

    @Test
    @DisplayName("CONTENT_REPORT + preset AuditType 可不提供 reason")
    void contentReport_presetType_reasonOptional() {
        CreateUserReportReq req = new CreateUserReportReq();
        req.setTicketType(TicketType.CONTENT_REPORT);
        req.setTargetId("123");
        req.setTargetType(TargetType.POST);
        // preset type should allow empty reason
        assertTrue(req.isReasonValid(),
                "CONTENT_REPORT + preset AuditType 无 reason 应通过校验");
    }

    @Test
    @DisplayName("CONTENT_REPORT 没有 targetId 应不通过")
    void contentReport_noTargetId_shouldFail() {
        CreateUserReportReq req = new CreateUserReportReq();
        req.setTicketType(TicketType.CONTENT_REPORT);
        req.setTargetType(TargetType.POST);
        // targetId is null
        assertFalse(req.isTargetValidForReport(),
                "CONTENT_REPORT 无 targetId 应不通过校验");
    }

    @Test
    @DisplayName("CONTENT_REPORT targetType=DEFAULT 应不通过")
    void contentReport_defaultTargetType_shouldFail() {
        CreateUserReportReq req = new CreateUserReportReq();
        req.setTicketType(TicketType.CONTENT_REPORT);
        req.setTargetId("123");
        req.setTargetType(TargetType.DEFAULT);
        assertFalse(req.isTargetValidForReport(),
                "CONTENT_REPORT + DEFAULT targetType 应不通过校验");
    }

    @Test
    @DisplayName("SUGGESTION 必须提供 reason")
    void suggestion_requiresReason() {
        CreateUserReportReq req = new CreateUserReportReq();
        req.setTicketType(TicketType.SUGGESTION);
        assertFalse(req.isReasonValid(),
                "SUGGESTION 无 reason 应不通过校验");
    }

    @Test
    @DisplayName("FEATURE_FEEDBACK 必须提供 reason")
    void featureFeedback_requiresReason() {
        CreateUserReportReq req = new CreateUserReportReq();
        req.setTicketType(TicketType.FEATURE_FEEDBACK);
        assertFalse(req.isReasonValid(),
                "FEATURE_FEEDBACK 无 reason 应不通过校验");
    }

    @Test
    @DisplayName("ACCOUNT_APPEAL 必须提供 reason")
    void accountAppeal_requiresReason() {
        CreateUserReportReq req = new CreateUserReportReq();
        req.setTicketType(TicketType.ACCOUNT_APPEAL);
        assertFalse(req.isReasonValid(),
                "ACCOUNT_APPEAL 无 reason 应不通过校验");
    }
}
