package org.waterwood.waterfunservicecore.infrastructure.utils.generator;

import com.ibm.icu.text.Transliterator;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.waterwood.common.constratin.UniquenessChecker;
import org.waterwood.common.exceptions.ServiceException;
import org.waterwood.utils.StringUtil;
import org.waterwood.utils.codec.HashUtil;
import org.waterwood.utils.generator.IdentifierGenerator;

import java.util.regex.Pattern;

@Component
public class ICUSlugGenerator implements IdentifierGenerator {
    private static final Transliterator TR = Transliterator.getInstance("Any-Latin; Latin-ASCII");
    private static final Pattern DUP_HYPHEN = Pattern.compile("-+");
    private static final Pattern INVALID   = Pattern.compile("[^a-z0-9-]");
    @Override
    @Transactional
    public String generateSlug(String raw, UniquenessChecker checker) {
        String base = toSlug(raw);
        return uniquify(base, checker);
    }

    @Override
    @Transactional
    public String generateCode(String raw, UniquenessChecker checker) {
        String base = toCode(raw);
        return uniquify(base, checker);
    }

    @Override
    @Transactional
    public String uniquify(String base, UniquenessChecker checker) {
        if (! checker.exist(base)) return base;
        String slug = base;
        for(int i = 0; i < 3; i++){
            if(i > 0) slug = base + "-" + HashUtil.next62_6();
            try{
                if(! checker.exist(slug)) return slug;
            }catch (DataIntegrityViolationException e){
                continue;
            }
        }
        throw new ServiceException("Slug conflict after 3 retries");
    }
    /**
     * Generate a slug from raw string
     * <b>WILL NOT CHECK uniquifition</b>
     * @param raw raw string
     * @return slug
     */
    public static String toSlug(String raw){
        if(StringUtil.isBlank(raw)) return "untitled";
        String ascii = TR.transliterate(raw.trim());
        String slug = ascii.toLowerCase()
                .replaceAll(INVALID.pattern(), "-")
                .replaceAll(DUP_HYPHEN.pattern(), "-")
                .replaceAll("^-|-$", "");
        return slug.isEmpty() ? "untitled" : slug;
    }

    public static String toCode(String raw){
        if(StringUtil.isBlank(raw)) return "UNTITLED";
        String ascii = TR.transliterate(raw.trim());
        String slug = ascii.toUpperCase()
                .replaceAll(INVALID.pattern(), "_")
                .replaceAll(DUP_HYPHEN.pattern(), "_")
                .replaceAll("^-|-$", "");
        return slug.isEmpty() ? "untitled" : slug;
    }
}
