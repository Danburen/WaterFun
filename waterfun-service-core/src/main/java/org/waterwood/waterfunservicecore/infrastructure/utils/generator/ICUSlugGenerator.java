package org.waterwood.waterfunservicecore.infrastructure.utils.generator;

import com.ibm.icu.text.Transliterator;
import org.jetbrains.annotations.NotNull;
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
    private static final Pattern DUP_UNDERSCORE = Pattern.compile("_+");
    private static final Pattern INVALID_CODE   = Pattern.compile("[^A-Z0-9_]");

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
        if (! checker.existsWithUniqueIdentify(base)) return base;
        String identify = base;
        for(int i = 0; i < 3; i++){
            if(i > 0) identify = base + "-" + HashUtil.next62_6();
            try{
                if(! checker.existsWithUniqueIdentify(identify)) return identify;
            }catch (DataIntegrityViolationException e){
               // continue;
            }
        }
        throw new ServiceException("Slug conflict after 3 retries");
    }

    @Override
    @Transactional
    public String fromCode(String code, @NotNull String fallback, UniquenessChecker checker) {
        if(StringUtil.isBlank(code)) return generateCode(fallback, checker);
        if(! checker.existsWithUniqueIdentify(code)) return code;
        return generateCode(fallback, checker);
    }

    @Override
    @Transactional
    public String fromSlug(String slug, @NotNull String fallback, UniquenessChecker checker) {
        if(StringUtil.isBlank(slug)) return generateSlug(fallback, checker);
        if(! checker.existsWithUniqueIdentify(slug)) return slug;
        return generateCode(fallback, checker);
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
        String separated = ascii
                .replaceAll("(?<=[a-z])(?=[A-Z])", "-")
                .replaceAll("(?<=[A-Z])(?=[A-Z][a-z])", "-")
                .replaceAll("(?<=[a-zA-Z])(?=[0-9])", "-")
                .replaceAll("(?<=[0-9])(?=[a-zA-Z])", "-");
        String slug = separated.toLowerCase()
                .replaceAll(INVALID.pattern(), "-")
                .replaceAll(DUP_HYPHEN.pattern(), "-")
                .replaceAll("^-|-$", "");
        return slug.isEmpty() ? "untitled" : slug;
    }

    public static String toCode(String raw){
        if(StringUtil.isBlank(raw)) return "UNTITLED";
        String ascii = TR.transliterate(raw.trim());
        String separated = ascii
                .replaceAll("(?<=[a-z])(?=[A-Z])", "_")
                .replaceAll("(?<=[A-Z])(?=[A-Z][a-z])", "_")
                .replaceAll("(?<=[a-zA-Z])(?=[0-9])", "_")
                .replaceAll("(?<=[0-9])(?=[a-zA-Z])", "_");
        String code = separated.toUpperCase()
                .replaceAll(INVALID_CODE.pattern(), "_")
                .replaceAll(DUP_UNDERSCORE.pattern(), "_")
                .replaceAll("^_|_$", "");
        return code.isEmpty() ? "UNTITLED" : code;
    }
}
