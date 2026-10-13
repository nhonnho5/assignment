package com.studyanchor.tutor.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implements FR-07 AC4, AC9. Domain service: finds the material paragraphs that share the most words with a query,
 * so the tutor can ground its hints in the student's own notes. Plain word overlap, no embeddings.
 */
public final class MaterialSearch {

    /** A paragraph is a block between blank lines, cut at about this many characters (PDFs rarely have blank lines). */
    static final int MAX_PARAGRAPH = 1_000;
    private static final int RESULTS = 3;
    private static final Set<String> STOP_WORDS =
            Set.of("the", "and", "what", "which", "how", "why", "for", "with", "that", "this", "are", "does", "gets");

    public record Excerpt(String materialTitle, String text) {
    }

    private record Scored(Excerpt excerpt, long score) {
    }

    private MaterialSearch() {
    }

    /** Up to 3 paragraphs, best first (ties keep the material's order); none when no word is shared. */
    public static List<Excerpt> search(List<Material> materials, String query) {
        Set<String> wanted = words(query);
        return materials.stream()
                .flatMap(m -> paragraphs(m.content()).stream().map(p -> new Excerpt(m.title(), p)))
                .map(e -> new Scored(e, words(e.text()).stream().filter(wanted::contains).count()))
                .filter(s -> s.score() > 0)
                .sorted(Comparator.comparingLong(Scored::score).reversed())
                .limit(RESULTS)
                .map(Scored::excerpt)
                .toList();
    }

    /** Lower-case words without stop words; a trailing "s" is dropped so "partitions" matches "partition". */
    private static Set<String> words(String text) {
        return Arrays.stream(text.toLowerCase().split("[^\\p{L}\\p{N}]+"))
                .filter(w -> w.length() > 2 && !STOP_WORDS.contains(w))
                .map(w -> w.length() > 3 && w.endsWith("s") ? w.substring(0, w.length() - 1) : w)
                .collect(Collectors.toSet());
    }

    private static List<String> paragraphs(String text) {
        List<String> paragraphs = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String line : text.split("\\R")) {
            if (line.isBlank() || current.length() + line.length() >= MAX_PARAGRAPH) {
                flush(current, paragraphs);
            }
            if (!line.isBlank()) {
                current.append(line.strip()).append(' ');
            }
        }
        flush(current, paragraphs);
        return paragraphs;
    }

    private static void flush(StringBuilder current, List<String> paragraphs) {
        if (!current.isEmpty()) {
            paragraphs.add(current.toString().strip());
            current.setLength(0);
        }
    }
}
