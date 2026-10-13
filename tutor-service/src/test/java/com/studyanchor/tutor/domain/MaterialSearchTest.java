package com.studyanchor.tutor.domain;

import com.studyanchor.tutor.domain.MaterialSearch.Excerpt;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** FR-07 AC9: picking the material paragraphs that share the most words with a query. Pure Java. */
class MaterialSearchTest {

    private static Material material(String title, String content) {
        return Material.create("alice", UUID.randomUUID(), title + ".md", title, content,
                Instant.parse("2026-10-14T03:00:00Z"));
    }

    private final Material lecture = material("Lecture 3", """
            Kafka stores records in topics.

            A topic is split into partitions. Each partition is an ordered log.

            Consumers in a group share the partitions of a topic.

            The key decides the partition of a record.

            Brokers replicate partitions for fault tolerance.""");

    @Test
    void FR07_searchReturnsTop3ParagraphsByWordOverlap() {
        List<Excerpt> found = MaterialSearch.search(List.of(lecture), "which partition gets a record with a key?");

        assertThat(found).hasSize(3);
        assertThat(found.getFirst().text()).isEqualTo("The key decides the partition of a record.");
        assertThat(found).allSatisfy(e -> assertThat(e.materialTitle()).isEqualTo("Lecture 3"));
    }

    @Test
    void FR07_searchIgnoresCase() {
        assertThat(MaterialSearch.search(List.of(lecture), "BROKERS"))
                .extracting(Excerpt::text).containsExactly("Brokers replicate partitions for fault tolerance.");
    }

    @Test
    void FR07_searchFindsNothingWithoutSharedWords() {
        assertThat(MaterialSearch.search(List.of(lecture), "photosynthesis")).isEmpty();
        assertThat(MaterialSearch.search(List.of(), "partition")).isEmpty();
    }

    @Test
    void FR07_textWithoutBlankLinesIsSplitIntoShortParagraphs() {
        String line = "filler words about nothing in particular here ".repeat(4) + "\n";
        Material pdf = material("Slides", line.repeat(100) + "the offset marks a position\n" + line.repeat(100));

        List<Excerpt> found = MaterialSearch.search(List.of(pdf), "offset");

        assertThat(found).singleElement().satisfies(e -> {
            assertThat(e.text()).contains("offset");
            assertThat(e.text().length()).isLessThanOrEqualTo(MaterialSearch.MAX_PARAGRAPH);
        });
    }
}
