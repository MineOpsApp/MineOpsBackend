package MineOpsBackend.repository;

import MineOpsBackend.model.Notice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NoticeRepository extends JpaRepository<Notice, Long> {
    List<Notice> findAllByOrderByCreatedAtDesc();

    Page<Notice> findAllByOrderByCreatedAtDesc(Pageable pageable);
}