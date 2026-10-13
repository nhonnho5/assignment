package com.studyanchor.social.presentation;

import com.studyanchor.social.presentation.SocialDtos.FriendStudyingResponse;
import com.studyanchor.social.presentation.SocialDtos.RoomStudyingResponse;
import com.studyanchor.social.presentation.SocialDtos.StudyingNowResponse;
import com.studyanchor.social.service.StudyingNowService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Implements FR-09 AC1, AC2. The live "studying now" board for friends and for a study room. */
@RestController
class StudyingNowController {

    private final StudyingNowService service;

    StudyingNowController(StudyingNowService service) {
        this.service = service;
    }

    @GetMapping("/friends/studying-now")
    StudyingNowResponse friends(@RequestHeader("X-User-Id") String userId) {
        return new StudyingNowResponse(userId,
                service.friendsStudying(userId).stream().map(FriendStudyingResponse::from).toList());
    }

    @GetMapping("/rooms/{roomId}/studying-now")
    RoomStudyingResponse room(@RequestHeader("X-User-Id") String userId, @PathVariable UUID roomId) {
        return RoomStudyingResponse.from(service.roomStudying(userId, roomId));
    }
}
