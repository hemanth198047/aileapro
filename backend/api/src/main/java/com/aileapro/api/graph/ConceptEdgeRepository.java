package com.aileapro.api.graph;

import com.aileapro.domain.ContentStatus;
import com.aileapro.domain.EdgeType;
import com.aileapro.persistence.ConceptEdgeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConceptEdgeRepository extends JpaRepository<ConceptEdgeEntity, UUID> {

    Optional<ConceptEdgeEntity> findByFromIdAndToIdAndEdgeType(UUID fromId, UUID toId, EdgeType edgeType);

    List<ConceptEdgeEntity> findByEdgeTypeAndStatus(EdgeType edgeType, ContentStatus status);

    /** Incoming edges of one type: used to walk prerequisites backwards from a target. */
    List<ConceptEdgeEntity> findByToIdInAndEdgeTypeAndStatus(
            Collection<UUID> toIds, EdgeType edgeType, ContentStatus status);

    /** Outgoing edges of one type: used to walk dependents forwards for impact. */
    List<ConceptEdgeEntity> findByFromIdInAndEdgeTypeAndStatus(
            Collection<UUID> fromIds, EdgeType edgeType, ContentStatus status);

    /**
     * Every edge of the requested types with at least one end in {@code ids}. The
     * neighbourhood walk expands one hop at a time, so this is called with the frontier
     * rather than with the whole graph.
     */
    @Query("""
            select e from ConceptEdgeEntity e
            where e.status = :status
              and e.edgeType in :types
              and (e.fromId in :ids or e.toId in :ids)
            """)
    List<ConceptEdgeEntity> findTouching(
            @Param("ids") Collection<UUID> ids,
            @Param("types") Collection<EdgeType> types,
            @Param("status") ContentStatus status);

    long countByStatus(ContentStatus status);
}
