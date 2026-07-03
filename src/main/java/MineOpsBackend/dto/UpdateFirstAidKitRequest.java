package MineOpsBackend.dto;

public record UpdateFirstAidKitRequest(
    String zone,
    String location,
    boolean hasBandages,
    boolean hasGloves,
    boolean hasAntiseptic,
    boolean hasOxygen,
    boolean hasStretcher,
    String notes
) {}
