package com.rinha2025.__Imp_rinha2025.repository;

import com.rinha2025.__Imp_rinha2025.entity.PaymentEntity;
import com.rinha2025.__Imp_rinha2025.model.projection.PaymentSummaryProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<PaymentEntity, Long> {

    @Query(value = """
                SELECT 
                    p.is_default AS isDefault,
                    COUNT(p) AS totalRequests,
                    COALESCE(SUM(p.amount), 0) AS totalAmount
                FROM payments p
                WHERE
                    p.createdAt BETWEEN :from AND :to
                GROUP BY p.is_default
            """, nativeQuery = true)
    List<PaymentSummaryProjection> findSummaryByDateRange(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );
}
