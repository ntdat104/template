package io.tcbs.template.repository;

import io.tcbs.template.enums.OutboxStatus;
import io.tcbs.template.model.OutboxEvent;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
    @Modifying
    @Transactional
    @Query(
        """
        UPDATE OutboxEvent e
        SET e.status = :status
        WHERE e.code = :code
        """
    )
    int markStatus(@Param("code") String code, @Param("status") OutboxStatus status);

    @Modifying
    @Transactional
    @Query(
        """
        UPDATE OutboxEvent e
        SET e.status = :status, e.publishedAt = :publishedAt
        WHERE e.code = :code
        """
    )
    int markStatus(@Param("code") String code, @Param("status") OutboxStatus status, @Param("publishedAt") Instant publishedAt);
}
