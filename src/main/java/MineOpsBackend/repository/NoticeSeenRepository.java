package MineOpsBackend.repository;

import MineOpsBackend.model.NoticeSeen;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NoticeSeenRepository extends JpaRepository<NoticeSeen, Long> {
    boolean existsByNoticeIdAndEmailIgnoreCase(Long noticeId, String email);

    List<NoticeSeen> findByNoticeIdOrderBySeenAtDesc(Long noticeId);
}
