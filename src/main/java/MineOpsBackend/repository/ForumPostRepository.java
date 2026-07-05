package MineOpsBackend.repository;

import MineOpsBackend.model.ForumPost;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ForumPostRepository extends JpaRepository<ForumPost, Long> {
    List<ForumPost> findBySubforumOrderByCreatedAtDesc(String subforum);
    List<ForumPost> findBySubforumAndCategoryOrderByCreatedAtDesc(String subforum, String category);
    List<ForumPost> findAllByOrderByCreatedAtDesc();
}
