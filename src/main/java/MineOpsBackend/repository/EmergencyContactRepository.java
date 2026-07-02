package MineOpsBackend.repository;

import MineOpsBackend.model.EmergencyContact;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmergencyContactRepository extends JpaRepository<EmergencyContact, Long> {

    List<EmergencyContact> findByWorkerIdOrderByContactTypeAsc(Long workerId);

    Optional<EmergencyContact> findByWorkerIdAndContactType(Long workerId, String contactType);
}
