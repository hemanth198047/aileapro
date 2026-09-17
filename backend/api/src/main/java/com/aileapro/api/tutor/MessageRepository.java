package com.aileapro.api.tutor;

import com.aileapro.persistence.MessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<MessageEntity, UUID> {

    List<MessageEntity> findByConversationIdOrderBySortAsc(UUID conversationId);

    /**
     * Used when a learner deletes a conversation.
     *
     * <p>Explicit rather than left to the foreign key's {@code ON DELETE CASCADE}. The cascade
     * is real in Postgres and absent from the schema the tests generate, so relying on it would
     * mean the one thing worth being sure about — that a deleted transcript is actually gone —
     * is the thing no test could check.
     */
    void deleteByConversationId(UUID conversationId);

    @Query("select coalesce(max(m.sort), -1) from MessageEntity m where m.conversationId = :conversationId")
    int maxSort(@Param("conversationId") UUID conversationId);

    @Query("select count(m) from MessageEntity m where m.conversationId = :conversationId and m.role = 'user'")
    int countLearnerTurns(@Param("conversationId") UUID conversationId);
}
