package com.studyanchor.tutor.domain;

/**
 * Implements FR-02 AC4. Port: turns an uploaded file into plain text (.txt and .md as UTF-8, text extracted
 * from .pdf); implemented in infrastructure. Throws {@link DomainException} for other or unreadable files.
 */
public interface MaterialText {

    String extract(String fileName, byte[] bytes);
}
