package org.waterwood.waterfunservicecore.services.content;

import com.github.houbb.sensitive.word.core.SensitiveWordHelper;
import org.springframework.stereotype.Service;

@Service
public class TextFilterService {

    public boolean containsSensitiveWords(String text) {
        if (text == null || text.isBlank()) return false;
        return SensitiveWordHelper.contains(text);
    }

    public String filterSensitiveWords(String text) {
        if (text == null || text.isBlank()) return text;
        return SensitiveWordHelper.replace(text);
    }

}
