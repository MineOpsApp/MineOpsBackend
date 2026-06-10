package MineOpsBackend.config;

import MineOpsBackend.model.Site;
import MineOpsBackend.repository.SiteRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private final SiteRepository siteRepository;

    public DataSeeder(SiteRepository siteRepository) {
        this.siteRepository = siteRepository;
    }

    @Override
    public void run(String... args) {
        if (siteRepository.count() > 0) {
            return;
        }

        siteRepository.save(new Site("Obuasi Mine", "Active"));
        siteRepository.save(new Site("Tarkwa Site", "Active"));
        siteRepository.save(new Site("Ahafo Operation", "Monitoring"));
    }
}
