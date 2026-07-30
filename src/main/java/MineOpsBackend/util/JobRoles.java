package MineOpsBackend.util;

import java.util.Set;

/**
 * Single source of truth for the structured "job role" values a supervisor can assign to a
 * worker (AppUser.jobRole), and which of those roles are eligible to start drill operations.
 * Kept separate from the free-text jobTitle field, which is unstructured HR data and unsuitable
 * for gating logic. The frontend's job-role picker (WorkerProfileViewScreen) must offer exactly
 * this set of values so nothing gets saved that DrillController can't recognize.
 *
 * A worker with jobRole == null is treated as eligible for drill operations everywhere this is
 * checked — this is intentional so existing workers aren't retroactively locked out of something
 * they could already do before this field existed. Only an explicit, non-eligible jobRole
 * restricts access.
 */
public final class JobRoles {

    private JobRoles() {}

    public static final String DRILL_OPERATOR = "DRILL_OPERATOR";
    public static final String BLASTER = "BLASTER";
    public static final String LOADER_OPERATOR = "LOADER_OPERATOR";
    public static final String HAULER = "HAULER";
    public static final String GENERAL_LABORER = "GENERAL_LABORER";
    public static final String SECURITY = "SECURITY";
    public static final String MAINTENANCE = "MAINTENANCE";
    public static final String SURVEYOR = "SURVEYOR";
    public static final String OTHER = "OTHER";

    public static final Set<String> ALL = Set.of(
        DRILL_OPERATOR, BLASTER, LOADER_OPERATOR, HAULER,
        GENERAL_LABORER, SECURITY, MAINTENANCE, SURVEYOR, OTHER
    );

    // Only these job roles may start a drill operation. Extend this set (e.g. add BLASTER) if
    // the site's real workflow turns out to need it — this is the one place to change.
    public static final Set<String> DRILL_ELIGIBLE = Set.of(DRILL_OPERATOR);

    public static boolean isDrillEligible(String jobRole) {
        return jobRole == null || DRILL_ELIGIBLE.contains(jobRole);
    }
}
