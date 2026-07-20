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

    Page<Notice> findBySiteIgnoreCaseOrderByCreatedAtDesc(String site, Pageable pageable);

    long countBySiteIgnoreCase(String site);

    @Query("SELECT n FROM Notice n WHERE (n.expiresAt IS NULL OR n.expiresAt > :now) AND (n.site IS NULL OR LOWER(n.site) = LOWER(:site)) ORDER BY n.createdAt DESC")
    Page<Notice> findActiveNoticesBySite(@org.springframework.data.repository.query.Param("now") java.time.LocalDateTime now, @org.springframework.data.repository.query.Param("site") String site, Pageable pageable);

    @Query("SELECT n FROM Notice n WHERE (n.expiresAt IS NULL OR n.expiresAt > :now) AND (n.site IS NULL OR LOWER(n.site) = LOWER(:site)) AND n.createdAt >= :userCreatedAt ORDER BY n.createdAt DESC")
    Page<Notice> findActiveNoticesBySiteForUser(@org.springframework.data.repository.query.Param("now") java.time.LocalDateTime now, @org.springframework.data.repository.query.Param("site") String site, @org.springframework.data.repository.query.Param("userCreatedAt") java.time.LocalDateTime userCreatedAt, Pageable pageable);
}