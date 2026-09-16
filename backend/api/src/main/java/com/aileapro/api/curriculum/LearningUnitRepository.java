package com.aileapro.api.curriculum;

import com.aileapro.domain.ContentStatus;
import com.aileapro.persistence.LearningUnitEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LearningUnitRepository extends JpaRepository<LearningUnitEntity, UUID> {

    Optional<LearningUnitEntity> findBySlugAndLocale(String slug, String locale);

    List<LearningUnitEntity> findByParentIdAndStatusOrderByTitleAsc(UUID parentId, ContentStatus status);

    List<LearningUnitEntity> findByIdInAndStatus(Collection<UUID> ids, ContentStatus status);

    /** Published child counts for a batch of units, used to show "contains N units" without N queries. */
    @Query("""
            select u.parentId, count(u) from LearningUnitEntity u
            where u.parentId in :ids and u.status = :status
            group by u.parentId
            """)
    List<Object[]> countPublishedChildren(@Param("ids") Collection<UUID> ids, @Param("status") ContentStatus status);

    long countByStatus(ContentStatus status);
}
