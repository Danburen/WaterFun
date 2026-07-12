package org.waterwood.waterfunservicecore.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.waterwood.waterfunservicecore.entity.security.PenaltyType;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BanPermission 枚举映射测试")
public class BanPermissionMappingTest {

    @Test
    @DisplayName("所有 BanPermission 应能正确映射到 PenaltyType")
    void allBanPermissions_mapToValidPenaltyType() {
        for (BanPermission bp : BanPermission.values()) {
            PenaltyType pt = bp.getPenaltyType();
            assertNotNull(pt, "BanPermission." + bp.name() + " 映射到 null PenaltyType");
            assertNotEquals(PenaltyType.OTHER, pt,
                    "BanPermission." + bp.name() + " 不应映射到 OTHER（非预期回退）");
        }
    }

    @Test
    @DisplayName("BanPermission.BAN_LOGIN 的 code 应为 ban:login")
    void banLogin_codeIsBanLogin() {
        assertEquals("ban:login", BanPermission.BAN_LOGIN.getCode());
    }

    @Test
    @DisplayName("BanPermission.BAN_POST 的 code 应为 ban:post")
    void banPost_codeIsBanPost() {
        assertEquals("ban:post", BanPermission.BAN_POST.getCode());
    }

    @Test
    @DisplayName("BanPermission.BAN_COMMENT 的 code 应为 ban:comment")
    void banComment_codeIsBanComment() {
        assertEquals("ban:comment", BanPermission.BAN_COMMENT.getCode());
    }

    @Test
    @DisplayName("BanPermission.BAN_UPLOAD 的 code 应为 ban:upload")
    void banUpload_codeIsBanUpload() {
        assertEquals("ban:upload", BanPermission.BAN_UPLOAD.getCode());
    }

    @Test
    @DisplayName("BanPermission.BAN_CHAT 的 code 应为 ban:chat")
    void banChat_codeIsBanChat() {
        assertEquals("ban:chat", BanPermission.BAN_CHAT.getCode());
    }

    @Test
    @DisplayName("BanPermission.BAN_CREATE 的 code 应为 ban:create")
    void banCreate_codeIsBanCreate() {
        assertEquals("ban:create", BanPermission.BAN_CREATE.getCode());
    }

    @Test
    @DisplayName("所有 BanPermission 的 code 都以 ban: 前缀开头")
    void allCodes_startWithBanPrefix() {
        for (BanPermission bp : BanPermission.values()) {
            assertTrue(bp.getCode().startsWith("ban:"),
                    "BanPermission." + bp.name() + " code 不以 ban: 开头: " + bp.getCode());
        }
    }

    @Test
    @DisplayName("PenaltyType 值应与 BanPermission 一一对应")
    void penaltyType_bijectionWithBanPermission() {
        assertEquals(BanPermission.values().length, PenaltyType.values().length - 2, // -2 for UNSPECIFIED and OTHER
                "BanPermission 与 PenaltyType 数量不匹配（不含 UNSPECIFIED/OTHER）");
    }
}
