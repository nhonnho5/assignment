package com.studyanchor.social.presentation;

import com.studyanchor.social.domain.StudyRoom;
import com.studyanchor.social.presentation.SocialDtos.CreateRoomRequest;
import com.studyanchor.social.presentation.SocialDtos.RoomResponse;
import com.studyanchor.social.service.StudyRoomService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

/** Implements FR-08 AC2. REST endpoints for study rooms; delegates all decisions to the service. */
@RestController
@RequestMapping("/rooms")
class StudyRoomController {

    private final StudyRoomService service;

    StudyRoomController(StudyRoomService service) {
        this.service = service;
    }

    @PostMapping
    ResponseEntity<RoomResponse> create(@RequestHeader("X-User-Id") String userId,
                                        @RequestBody CreateRoomRequest request) {
        StudyRoom room = service.create(userId, request.name());
        return ResponseEntity.created(URI.create("/rooms/" + room.id())).body(RoomResponse.from(room));
    }

    @PostMapping("/{roomId}/join")
    RoomResponse join(@RequestHeader("X-User-Id") String userId, @PathVariable UUID roomId) {
        return RoomResponse.from(service.join(userId, roomId));
    }

    @GetMapping("/{roomId}")
    RoomResponse get(@RequestHeader("X-User-Id") String userId, @PathVariable UUID roomId) {
        return RoomResponse.from(service.get(userId, roomId));
    }
}
