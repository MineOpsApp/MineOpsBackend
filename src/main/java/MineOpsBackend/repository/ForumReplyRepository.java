package MineOpsBackend.repository;

import MineOpsBackend.model.ForumReply;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ForumReplyRepository extends JpaRepository<ForumReply, Long> {
    List<ForumReply> findByPostIdOrderByCreatedAtAsc(Long postId);
}
