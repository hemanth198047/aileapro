package com.aileapro.api.tutor;

import com.aileapro.persistence.ConversationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConversationRepository extends JpaRepository<ConversationEntity, UUID> {

    /**
     * Always fetched with the owner in the query rather than checked afterwards.
     *
     * <p>A conversation is the most private thing this product stores, and "load by id, then
     * compare the user" is one forgotten line away from serving somebody else's transcript.
     */
    Optional<ConversationEntity> findByIdAndUserId(UUID id, UUID userId);

    List<ConversationEntity> findTop20ByUserIdOrderByLastMessageAtDescCreatedAtDesc(UUID userId);

    List<ConversationEntity> findByUserIdAndUnitIdOrderByCreatedAtDesc(UUID userId, UUID unitId);
}
