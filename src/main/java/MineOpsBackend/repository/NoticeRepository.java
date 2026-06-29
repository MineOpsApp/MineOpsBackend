package MineOpsBackend.repository;

import MineOpsBackend.model.Notice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.List;

public interface NoticeRepository extends JpaRepository<Notice, Long> {
    List<Notice> findAllByOrderByCreatedAtDesc();

    Page<Notice> findAllByOrderByCreatedAtDesc(Pageable pageable);
    @Query("SELECT n FROM Notice n WHERE n.expiresAt IS NULL OR n.expiresAt > :now ORDER BY n.createdAt DESC")
Page<Notice> findActiveNotices(@org.springframework.data.repository.query.Param("now") java.time.LocalDateTime now, Pageable pageable);
}