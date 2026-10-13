package com.studyanchor.tutor.presentation;

import com.studyanchor.tutor.domain.Material;
import com.studyanchor.tutor.service.MaterialService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Implements FR-02. Upload, list and read study material; delegates all decisions to the service. */
@RestController
class MaterialController {

    /** List view: no content (AC6). */
    record MaterialSummary(UUID materialId, UUID topicId, String title, int characters, Instant addedAt) {

        static MaterialSummary from(Material m) {
            return new MaterialSummary(m.id(), m.topicId(), m.title(), m.content().length(), m.addedAt());
        }
    }

    record MaterialResponse(UUID materialId, UUID topicId, String title, int characters, Instant addedAt,
                            String content) {

        static MaterialResponse from(Material m) {
            return new MaterialResponse(m.id(), m.topicId(), m.title(), m.content().length(), m.addedAt(),
                    m.content());
        }
    }

    private final MaterialService service;

    MaterialController(MaterialService service) {
        this.service = service;
    }

    /** Multipart form: {@code file} (.txt, .md or .pdf) and an optional {@code title} (AC4). */
    @PostMapping("/topics/{topicId}/materials")
    @ResponseStatus(HttpStatus.CREATED)
    MaterialSummary add(@RequestHeader("X-User-Id") String userId, @PathVariable UUID topicId,
                        @RequestParam("file") MultipartFile file,
                        @RequestParam(required = false) String title) throws IOException {
        return MaterialSummary.from(service.add(userId, topicId, file.getOriginalFilename(), file.getBytes(), title));
    }

    @GetMapping("/topics/{topicId}/materials")
    List<MaterialSummary> list(@RequestHeader("X-User-Id") String userId, @PathVariable UUID topicId) {
        return service.list(userId, topicId).stream().map(MaterialSummary::from).toList();
    }

    @GetMapping("/materials/{materialId}")
    MaterialResponse get(@RequestHeader("X-User-Id") String userId, @PathVariable UUID materialId) {
        return MaterialResponse.from(service.get(userId, materialId));
    }
}
