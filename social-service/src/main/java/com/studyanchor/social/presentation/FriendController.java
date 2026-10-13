package com.studyanchor.social.presentation;

import com.studyanchor.social.domain.Friendship;
import com.studyanchor.social.presentation.SocialDtos.FriendRequestRequest;
import com.studyanchor.social.presentation.SocialDtos.FriendRequestResponse;
import com.studyanchor.social.presentation.SocialDtos.FriendsResponse;
import com.studyanchor.social.service.FriendshipService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/** Implements FR-08 AC1. REST endpoints for friend requests and friends; delegates all decisions to the service. */
@RestController
@RequestMapping("/friends")
class FriendController {

    private final FriendshipService service;

    FriendController(FriendshipService service) {
        this.service = service;
    }

    @PostMapping("/requests")
    ResponseEntity<FriendRequestResponse> send(@RequestHeader("X-User-Id") String userId,
                                               @RequestBody FriendRequestRequest request) {
        Friendship sent = service.sendRequest(userId, request.toUserId());
        return ResponseEntity.created(URI.create("/friends/requests/" + sent.id()))
                .body(FriendRequestResponse.from(sent));
    }

    /** Pending requests sent to the current user, so they can find the ID to accept. */
    @GetMapping("/requests")
    List<FriendRequestResponse> incoming(@RequestHeader("X-User-Id") String userId) {
        return service.incomingRequests(userId).stream().map(FriendRequestResponse::from).toList();
    }

    @PostMapping("/requests/{requestId}/accept")
    FriendRequestResponse accept(@RequestHeader("X-User-Id") String userId, @PathVariable UUID requestId) {
        return FriendRequestResponse.from(service.accept(userId, requestId));
    }

    @GetMapping
    FriendsResponse friends(@RequestHeader("X-User-Id") String userId) {
        return new FriendsResponse(userId, service.friendsOf(userId));
    }
}
