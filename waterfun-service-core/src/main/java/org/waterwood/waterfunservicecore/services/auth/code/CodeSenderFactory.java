package org.waterwood.waterfunservicecore.services.auth.code;

import org.springframework.stereotype.Component;
import org.waterwood.api.BaseResponseCode;
import org.waterwood.common.exceptions.BizException;
import org.waterwood.waterfunservicecore.api.VerifyChannel;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class CodeSenderFactory {
    private final Map<VerifyChannel, CodeSender> senderMap;

    public CodeSenderFactory(List<CodeSender> senders) {
        senderMap = senders.stream()
                .collect(Collectors.toUnmodifiableMap(CodeSender::channel, Function.identity()));
    }

    public CodeSender of(VerifyChannel channel) {
        return Optional.ofNullable(senderMap.get(channel))
                .orElseThrow(() -> new BizException(BaseResponseCode.CHANNEL_NOT_SUPPORT, channel.getValue()));
    }
}
