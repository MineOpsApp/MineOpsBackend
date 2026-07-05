package MineOpsBackend.controller;

import MineOpsBackend.model.AppUser;
import MineOpsBackend.model.ForumPost;
import MineOpsBackend.repository.AppUserRepository;
import MineOpsBackend.repository.ForumPostRepository;
import MineOpsBackend.repository.ForumReplyRepository;
import MineOpsBackend.security.AuthenticatedUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ForumControllerTest {

    @Mock ForumPostRepository postRepo;
    @Mock ForumReplyRepository replyRepo;
    @Mock AppUserRepository userRepo;

    @InjectMocks ForumController controller;

    private static final String SITE = "Obuasi Mine";

    private AuthenticatedUser supervisor() {
        return new AuthenticatedUser(1L, "Kwame", "kwame@mine.com", "supervisor", SITE, null);
    }

    private AuthenticatedUser worker() {
        return new AuthenticatedUser(2L, "Kofi", "kofi@mine.com", "worker", SITE, null);
    }

    private AuthenticatedUser verifiedBuyer() {
        return new AuthenticatedUser(10L, "Ama", "ama@buyer.com", "buyer", null, null);
    }

    private AuthenticatedUser unverifiedBuyer() {
        return new AuthenticatedUser(11L, "Kojo", "kojo@buyer.com", "buyer", null, null);
    }

    private AppUser buyerRecord(String email, String verificationStatus) {
        AppUser u = new AppUser("Buyer", email, "hash", "buyer", null);
        u.setBuyerVerificationStatus(verificationStatus);
        return u;
    }

    // ── getPosts — mine_operator gating ───────────────────────────────────────

    @Test
    void getPosts_throws403_forMineOperatorSubforum_whenNotSupervisor() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.getPosts("mine_operator", null, worker()));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(postRepo, never()).findBySubforumOrderByCreatedAtDesc(any());
    }

    @Test
    void getPosts_allows_forMineOperatorSubforum_whenSupervisor() {
        when(postRepo.findBySubforumOrderByCreatedAtDesc("mine_operator")).thenReturn(List.of());

        List<ForumPost> result = controller.getPosts("mine_operator", null, supervisor());

        assertThat(result).isEmpty();
        verify(postRepo).findBySubforumOrderByCreatedAtDesc("mine_operator");
    }

    // ── getPosts — buyer subforum gating ─────────────────────────────────────

    @Test
    void getPosts_throws403_forBuyerSubforum_whenNotBuyer() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.getPosts("buyer", null, worker()));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void getPosts_throws403_forBuyerSubforum_whenBuyerNotVerified() {
        AuthenticatedUser buyer = unverifiedBuyer();
        when(userRepo.findById(buyer.id()))
                .thenReturn(Optional.of(buyerRecord(buyer.email(), "PENDING_VERIFICATION")));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.getPosts("buyer", null, buyer));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(postRepo, never()).findBySubforumOrderByCreatedAtDesc(any());
    }

    @Test
    void getPosts_allows_forBuyerSubforum_whenBuyerVerified() {
        AuthenticatedUser buyer = verifiedBuyer();
        when(userRepo.findById(buyer.id()))
                .thenReturn(Optional.of(buyerRecord(buyer.email(), "VERIFIED")));
        when(postRepo.findBySubforumOrderByCreatedAtDesc("buyer")).thenReturn(List.of());

        controller.getPosts("buyer", null, buyer);

        verify(postRepo).findBySubforumOrderByCreatedAtDesc("buyer");
    }

    @Test
    void getPosts_noGating_whenSubforumIsNull() {
        when(postRepo.findAllByOrderByCreatedAtDesc()).thenReturn(List.of());

        controller.getPosts(null, null, worker());

        verify(postRepo).findAllByOrderByCreatedAtDesc();
    }

    // ── createPost — subforum gating ──────────────────────────────────────────

    @Test
    void createPost_throws403_forMineOperatorSubforum_whenWorker() {
        Map<String, String> body = Map.of("title", "T", "body", "B", "subforum", "mine_operator");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.createPost(worker(), body));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(postRepo, never()).save(any());
    }

    @Test
    void createPost_throws403_forBuyerSubforum_whenUnverifiedBuyer() {
        AuthenticatedUser buyer = unverifiedBuyer();
        when(userRepo.findById(buyer.id()))
                .thenReturn(Optional.of(buyerRecord(buyer.email(), "PENDING_VERIFICATION")));
        Map<String, String> body = Map.of("title", "T", "body", "B", "subforum", "buyer");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.createPost(buyer, body));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(postRepo, never()).save(any());
    }

    @Test
    void createPost_savesPost_forGeneralSubforum() {
        ForumPost saved = new ForumPost();
        when(postRepo.save(any(ForumPost.class))).thenReturn(saved);
        Map<String, String> body = Map.of("title", "My Post", "body", "Content", "subforum", "general");

        ForumPost result = controller.createPost(worker(), body);

        assertThat(result).isEqualTo(saved);
        ArgumentCaptor<ForumPost> cap = ArgumentCaptor.forClass(ForumPost.class);
        verify(postRepo).save(cap.capture());
        assertThat(cap.getValue().getTitle()).isEqualTo("My Post");
        assertThat(cap.getValue().getSubforum()).isEqualTo("general");
    }

    @Test
    void createPost_throws400_whenTitleMissing() {
        Map<String, String> body = Map.of("body", "B");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.createPost(worker(), body));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void createPost_throws400_whenBodyMissing() {
        Map<String, String> body = Map.of("title", "T");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.createPost(worker(), body));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
