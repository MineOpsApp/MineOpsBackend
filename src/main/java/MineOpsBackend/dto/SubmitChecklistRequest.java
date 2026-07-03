package MineOpsBackend.dto;

public record SubmitChecklistRequest(
    boolean ppeHelmet,
    boolean ppeBoots,
    boolean ppeGloves,
    boolean ppeVest,
    boolean equipmentChecked,
    boolean communicationDevice,
    boolean emergencyExitsClear,
    boolean hazardousMaterialsSecured
) {}
