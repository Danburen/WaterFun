package org.waterwood.waterfunservice.service.resource;

import java.util.Set;

public class LegalResourceConstants {
    // Resource files requiring authentication (whitelist)
    public static final Set<String> PROTECTED_FILES = Set.of("contact.md", "contact_en_US.md");

    private LegalResourceConstants() {}
}
