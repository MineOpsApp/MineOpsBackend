package MineOpsBackend.repository;

import MineOpsBackend.model.SiteMap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface SiteMapRepository extends JpaRepository<SiteMap, Long> {
    Optional<SiteMap> findBySiteIgnoreCase(String site);

    // Lightweight projection used for polling — deliberately excludes imageData so the
    // (potentially multi-hundred-KB) blob isn't loaded from the DB or sent over the wire
    // just to check whether the map has changed since the client last fetched it.
    interface SiteMapMeta {
        Long getId();
        String getUploadedBy();
        LocalDateTime getUploadedAt();
    }

    @Query("select m.id as id, m.uploadedBy as uploadedBy, m.uploadedAt as uploadedAt from SiteMap m where lower(m.site) = lower(:site)")
    Optional<SiteMapMeta> findMetaBySiteIgnoreCase(@Param("site") String site);
}
