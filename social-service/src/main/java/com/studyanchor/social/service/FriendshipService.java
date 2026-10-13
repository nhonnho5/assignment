package com.studyanchor.social.service;

import com.studyanchor.social.domain.ConflictException;
import com.studyanchor.social.domain.Friendship;
import com.studyanchor.social.domain.FriendshipRepository;
import com.studyanchor.social.domain.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

/**
 * Implements FR-08 AC1, AC5, AC6. Application service for friend requests. The "no duplicate request in
 * either direction" rule spans several Friendship aggregates, so it is checked here against the repository.
 */
@Service
public class FriendshipService {

    private final FriendshipRepository repository;
    private final Clock clock;

    public FriendshipService(FriendshipRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /** FR-08 AC1, AC5: self → 400 (from the aggregate); already friends or pending either way → 409. */
    @Transactional
    public Friendship sendRequest(String fromUserId, String toUserId) {
        Friendship request = Friendship.request(fromUserId, toUserId, clock.instant());
        if (repository.existsBetween(fromUserId, toUserId)) {
            throw new ConflictException("A friend request or friendship already exists between "
                    + fromUserId + " and " + toUserId);
        }
        return repository.save(request);
    }

    /** FR-08 AC6: only the recipient can accept; unknown or someone else's request → 404. */
    @Transactional
    public Friendship accept(String userId, UUID requestId) {
        Friendship request = repository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Friend request " + requestId + " not found"));
        request.accept(userId, clock.instant());
        return repository.save(request);
    }

    /** Pending requests this user can accept. */
    @Transactional(readOnly = true)
    public List<Friendship> incomingRequests(String userId) {
        return repository.findPendingFor(userId);
    }

    /** FR-08 AC6: the user IDs of this user's friends (friendship is mutual). */
    @Transactional(readOnly = true)
    public List<String> friendsOf(String userId) {
        return repository.findAcceptedFor(userId).stream().map(f -> f.otherThan(userId)).sorted().toList();
    }
}
