package com.studyanchor.social.infrastructure;

import com.studyanchor.social.domain.Friendship;
import com.studyanchor.social.domain.FriendshipRepository;
import com.studyanchor.social.domain.FriendshipStatus;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Implements FR-08. Adapter: fulfils the domain repository interface using Spring Data JPA and H2. */
@Repository
class JpaFriendshipRepositoryAdapter implements FriendshipRepository {

    private final SpringDataFriendshipRepository jpa;

    JpaFriendshipRepositoryAdapter(SpringDataFriendshipRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Friendship save(Friendship friendship) {
        return jpa.save(friendship);
    }

    @Override
    public Optional<Friendship> findById(UUID id) {
        return jpa.findById(id);
    }

    @Override
    public boolean existsBetween(String a, String b) {
        return jpa.existsBetween(a, b);
    }

    @Override
    public List<Friendship> findAcceptedFor(String userId) {
        return jpa.findByStatusFor(FriendshipStatus.ACCEPTED, userId);
    }

    @Override
    public List<Friendship> findPendingFor(String recipientId) {
        return jpa.findByRecipientIdAndStatus(recipientId, FriendshipStatus.PENDING);
    }
}
