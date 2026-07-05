package MineOpsBackend.repository;

import MineOpsBackend.model.ForumPost;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ForumPostRepository extends JpaRepository<ForumPost, Long> {
    List<ForumPost> findBySubforumOrderByCreatedAtDesc(String subforum);
    List<ForumPost> findBySubforumAndCategoryOrderByCreatedAtDesc(String subforum, String category);
    List<ForumPost> findAllByOrderByCreatedAtDesc();

    @Query("SELECT p FROM ForumPost p WHERE p.subforum = 'general' AND (LOWER(p.title) LIKE LOWER(CONCAT('%',:q,'%')) OR LOWER(p.body) LIKE LOWER(CONCAT('%',:q,'%'))) ORDER BY p.createdAt DESC")
    List<ForumPost> searchGeneral(@Param("q") String q, Pageable pageable);
}
