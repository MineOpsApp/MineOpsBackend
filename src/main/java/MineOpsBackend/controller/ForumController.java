package MineOpsBackend.controller;

import MineOpsBackend.model.ForumPost;
import MineOpsBackend.model.ForumReply;
import MineOpsBackend.repository.ForumPostRepository;
import MineOpsBackend.repository.ForumReplyRepository;
import MineOpsBackend.security.AuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/forum")
public class ForumController {

    private final ForumPostRepository postRepo;
    private final ForumReplyRepository replyRepo;

    public ForumController(ForumPostRepository postRepo, ForumReplyRepository replyRepo) {
        this.postRepo = postRepo;
        this.replyRepo = replyRepo;
    }

    @GetMapping("/posts")
    @PreAuthorize("isAuthenticated()")
    public List<ForumPost> getPosts(@RequestParam(required = false) String subforum,
                                    @RequestParam(required = false) String category) {
        if (subforum != null && category != null) {
            return postRepo.findBySubforumAndCategoryOrderByCreatedAtDesc(subforum, category);
        } else if (subforum != null) {
            return postRepo.findBySubforumOrderByCreatedAtDesc(subforum);
        }
        return postRepo.findAllByOrderByCreatedAtDesc();
    }

    @PostMapping("/posts")
    @PreAuthorize("isAuthenticated()")
    public ForumPost createPost(@AuthenticationPrincipal AuthenticatedUser user,
                                @RequestBody Map<String, String> body) {
        String title = body.get("title");
        String postBody = body.get("body");
        String subforum = body.get("subforum");
        String category = body.get("category");

        if (title == null || title.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "title is required");
        }
        if (postBody == null || postBody.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "body is required");
        }

        ForumPost post = new ForumPost();
        post.setTitle(title.trim());
        post.setBody(postBody.trim());
        post.setSubforum(subforum != null ? subforum.trim() : "general");
        post.setCategory(category != null ? category.trim() : "");
        post.setAuthorEmail(user.email());
        post.setAuthorName(user.fullName());
        post.setAuthorRole(user.role());
        post.setReplyCount(0);
        post.setCreatedAt(LocalDateTime.now());
        return postRepo.save(post);
    }

    @GetMapping("/posts/{postId}/replies")
    @PreAuthorize("isAuthenticated()")
    public List<ForumReply> getReplies(@PathVariable Long postId) {
        postRepo.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));
        return replyRepo.findByPostIdOrderByCreatedAtAsc(postId);
    }

    @PostMapping("/posts/{postId}/replies")
    @PreAuthorize("isAuthenticated()")
    public ForumReply createReply(@PathVariable Long postId,
                                  @AuthenticationPrincipal AuthenticatedUser user,
                                  @RequestBody Map<String, String> body) {
        ForumPost post = postRepo.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));

        String replyBody = body.get("body");
        if (replyBody == null || replyBody.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "body is required");
        }

        ForumReply reply = new ForumReply();
        reply.setPostId(postId);
        reply.setBody(replyBody.trim());
        reply.setAuthorEmail(user.email());
        reply.setAuthorName(user.fullName());
        reply.setAuthorRole(user.role());
        reply.setCreatedAt(LocalDateTime.now());
        ForumReply saved = replyRepo.save(reply);

        post.setReplyCount(post.getReplyCount() + 1);
        postRepo.save(post);

        return saved;
    }
}
