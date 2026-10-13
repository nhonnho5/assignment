package com.studyanchor.planner.infrastructure;

import com.studyanchor.planner.domain.ValueStatement;
import com.studyanchor.planner.domain.ValueStatementRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Implements FR-11. Adapter: fulfils the domain repository interface using Spring Data JPA and H2. */
@Repository
class JpaValueStatementRepositoryAdapter implements ValueStatementRepository {

    private final SpringDataValueStatementRepository jpa;

    JpaValueStatementRepositoryAdapter(SpringDataValueStatementRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public ValueStatement save(ValueStatement value) {
        return jpa.save(value);
    }

    @Override
    public Optional<ValueStatement> findByIdAndUserId(UUID id, String userId) {
        return jpa.findByIdAndUserId(id, userId);
    }

    @Override
    public List<ValueStatement> findByUserId(String userId) {
        return jpa.findByUserIdOrderByCreatedAt(userId);
    }

    @Override
    public void delete(ValueStatement value) {
        jpa.delete(value);
    }
}
