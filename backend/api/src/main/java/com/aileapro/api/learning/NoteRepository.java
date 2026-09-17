package com.aileapro.api.learning;

import com.aileapro.persistence.NoteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NoteRepository extends JpaRepository<NoteEntity, UUID> {

    List<NoteEntity> findByUserIdAndUnitIdOrderByCreatedAtDesc(UUID userId, UUID unitId);

    List<NoteEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);

    /** Scoped by owner so a guessed id cannot reach somebody else's note. */
    Optional<NoteEntity> findByIdAndUserId(UUID id, UUID userId);
}
