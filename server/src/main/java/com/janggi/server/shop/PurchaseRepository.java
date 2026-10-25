package com.janggi.server.shop;

import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PurchaseRepository extends JpaRepository<Purchase, String> {

    List<Purchase> findTop100ByOrderByCreatedAtDesc();

    /** from 이후 결제가 끝난 주문의 금액 합. */
    @Query("select coalesce(sum(p.amount), 0) from Purchase p where p.status = 'PAID' and p.paidAt >= :from")
    long paidSince(@Param("from") OffsetDateTime from);

    long countByStatus(Purchase.Status status);
}
