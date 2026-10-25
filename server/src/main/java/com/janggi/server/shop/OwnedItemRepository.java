package com.janggi.server.shop;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OwnedItemRepository extends JpaRepository<OwnedItem, Long> {

    List<OwnedItem> findByUserId(long userId);

    boolean existsByUserIdAndItemId(long userId, String itemId);
}
