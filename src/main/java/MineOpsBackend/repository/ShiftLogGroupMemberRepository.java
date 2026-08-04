package MineOpsBackend.repository;

import MineOpsBackend.model.ShiftLogGroupMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShiftLogGroupMemberRepository extends JpaRepository<ShiftLogGroupMember, Long> {
    List<ShiftLogGroupMember> findByShiftLogId(Long shiftLogId);
    List<ShiftLogGroupMember> findByShiftLogIdIn(List<Long> shiftLogIds);
}
