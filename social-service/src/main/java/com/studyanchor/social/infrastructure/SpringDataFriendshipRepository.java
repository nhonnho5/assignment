package com.studyanchor.social.infrastructure;

import com.studyanchor.social.domain.Friendship;
import com.studyanchor.social.domain.FriendshipStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/** Spring Data generates the SQL for derived method names; the two "either direction" queries are spelled out. */
interface SpringDataFriendshipRepository extends JpaRepository<Friendship, UUID> {

    @Query("""
            select count(f) > 0 from Friendship f
            where (f.requesterId = :a and f.recipientId = :b) or (f.requesterId = :b and f.recipientId = :a)""")
    boolean existsBetween(@Param("a") String a, @Param("b") String b);

    @Query("""
            select f from Friendship f
            where f.status = :status and (f.requesterId = :userId or f.recipientId = :userId)""")
    List<Friendship> findByStatusFor(@Param("status") FriendshipStatus status, @Param("userId") String userId);

    List<Friendship> findByRecipientIdAndStatus(String recipientId, FriendshipStatus status);
}
