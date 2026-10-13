package com.studyanchor.focus.domain;

import java.util.Collection;
import java.util.Locale;

/**
 * Implements FR-06 AC3, AC7. Value object: a website reduced to its host, so what students type can be compared,
 * e.g. {@code "https://www.YouTube.com/watch?v=1"} becomes {@code "youtube.com"}.
 */
public record Site(String host) {

    /** Cleans what the student typed: no scheme, {@code www.}, port or path. Returns null when nothing was given. */
    public static Site of(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String host = raw.trim().toLowerCase(Locale.ROOT)
                .replaceFirst("^[a-z][a-z0-9+.-]*://", "")
                .replaceFirst("[/:?#].*", "")
                .replaceFirst("^www\\.", "");
        if (host.isEmpty()) {
            throw new DomainException("site is not a website: " + raw.trim());
        }
        return new Site(host);
    }

    /** True if this host is one of the allowed hosts or a subdomain of one ({@code docs.google.com} by {@code google.com}). */
    public boolean isAllowedBy(Collection<String> allowedHosts) {
        return allowedHosts.stream().anyMatch(allowed -> host.equals(allowed) || host.endsWith("." + allowed));
    }
}
