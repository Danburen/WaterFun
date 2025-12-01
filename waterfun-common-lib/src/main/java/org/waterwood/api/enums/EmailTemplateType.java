package org.waterwood.api.enums;

import lombok.Getter;

@Getter
public enum EmailTemplateType{
    VERIFY_CODE("verify_code","WaterFun 账户验证","WaterFun<verify@mail.waterfun.top>"),
    PASSWORD_RESET("password_reset","WaterFun 密码重置","WaterFun<verify@mail.waterfun.top>"),;
    private final String templateKey;
    private final String subject;
    private final String defaultFrom;
    EmailTemplateType(String templateKey, String subject, String defaultFrom) {
        this.templateKey = templateKey;
        this.subject = subject;
        this.defaultFrom = defaultFrom;
    }
}