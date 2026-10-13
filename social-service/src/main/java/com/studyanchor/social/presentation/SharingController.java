package com.studyanchor.social.presentation;

import com.studyanchor.social.presentation.SocialDtos.SharingRequest;
import com.studyanchor.social.presentation.SocialDtos.SharingResponse;
import com.studyanchor.social.service.SharingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Implements FR-08 AC3. The current user's presence-sharing setting (off by default). */
@RestController
@RequestMapping("/sharing")
class SharingController {

    private final SharingService service;

    SharingController(SharingService service) {
        this.service = service;
    }

    @GetMapping
    SharingResponse get(@RequestHeader("X-User-Id") String userId) {
        return SharingResponse.from(service.get(userId));
    }

    @PutMapping
    SharingResponse set(@RequestHeader("X-User-Id") String userId, @RequestBody SharingRequest request) {
        return SharingResponse.from(service.set(userId, request.sharing()));
    }
}
