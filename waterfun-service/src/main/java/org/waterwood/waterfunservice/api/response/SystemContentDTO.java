package org.waterwood.waterfunservice.api.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SystemContentDTO implements NotificationContent {
    private String text;

    @Override
    public String getDisplayText() {
        return text;
    }
}
