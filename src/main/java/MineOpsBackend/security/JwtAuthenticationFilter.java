package MineOpsBackend.security;

import MineOpsBackend.model.AppUser;
import MineOpsBackend.repository.AppUserRepository;
import MineOpsBackend.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final AppUserRepository appUserRepository;
    private final JwtService jwtService;

    public JwtAuthenticationFilter(AppUserRepository appUserRepository, JwtService jwtService) {
        this.appUserRepository = appUserRepository;
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            JwtService.JwtClaims claims = jwtService.validateToken(authorizationHeader.substring(7));
            AppUser user = appUserRepository.findByEmailIgnoreCase(claims.email()).orElseThrow();

            if (!user.getId().equals(claims.userId()) || !user.getRole().equals(claims.role())) {
                throw new IllegalArgumentException("Token user does not match stored user");
            }
            if (Boolean.FALSE.equals(user.getActive()) || user.getDeletedAt() != null) {
                throw new IllegalArgumentException("Account is suspended or deleted");
            }

           AuthenticatedUser principal = new AuthenticatedUser(
    user.getId(),
    user.getFullName(),
    user.getEmail(),
    user.getRole(),
    user.getAssignedSite(),
    user.getGuestSubRole()
);
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority(principal.authority()))
            );
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception error) {
            SecurityContextHolder.clearContext();
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
