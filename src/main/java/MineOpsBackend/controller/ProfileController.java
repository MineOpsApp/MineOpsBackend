package MineOpsBackend.controller;

import MineOpsBackend.model.AppUser;
import MineOpsBackend.repository.AppUserRepository;
import MineOpsBackend.repository.CertificationRepository;
import MineOpsBackend.repository.EmergencyContactRepository;
import MineOpsBackend.repository.ShiftLogRepository;
import MineOpsBackend.security.AuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class ProfileController {

    private final AppUserRepository userRepo;
    private final ShiftLogRepository shiftLogRepo;
    private final CertificationRepository certRepo;
    private final EmergencyContactRepository ecRepo;

    public ProfileController(
        AppUserRepository userRepo,
        ShiftLogRepository shiftLogRepo,
        CertificationRepository certRepo,
        EmergencyContactRepository ecRepo
    ) {
        this.userRepo = userRepo;
        this.shiftLogRepo = shiftLogRepo;
        this.certRepo = certRepo;
        this.ecRepo = ecRepo;
    }

    @GetMapping("/api/profile")
    @PreAuthorize("isAuthenticated()")
    public Map<String, Object> getOwnProfile(@AuthenticationPrincipal AuthenticatedUser user) {
        AppUser appUser = userRepo.findById(user.id())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return buildProfile(appUser);
    }

    @PutMapping("/api/profile")
    @PreAuthorize("isAuthenticated()")
    public Map<String, Object> updateProfile(
        @AuthenticationPrincipal AuthenticatedUser user,
        @RequestBody Map<String, String> body
    ) {
        AppUser appUser = userRepo.findById(user.id())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (body.containsKey("photo")) {
            String photo = body.get("photo");
            if (photo != null && photo.length() > 2_000_000) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Photo is too large. Use a smaller image.");
            }
            appUser.setProfilePhoto(photo);
        }
        if (body.containsKey("bio")) {
            String bio = body.get("bio");
            if (bio != null && bio.length() > 500) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bio must be 500 characters or less.");
            }
            appUser.setBio(bio);
        }
        if (body.containsKey("momoNumber")) {
            String num = body.get("momoNumber");
            if (num != null && !num.isBlank() && !num.matches("\\d{10,15}"))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MoMo number must be 10–15 digits.");
            appUser.setMomoNumber(num == null || num.isBlank() ? null : num);
        }
        if (body.containsKey("momoNetwork")) {
            String net = body.get("momoNetwork");
            if (net != null && !net.isBlank()
                    && !java.util.Set.of("MTN", "TELECEL", "AIRTELTIGO").contains(net.toUpperCase()))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "momoNetwork must be MTN, TELECEL, or AIRTELTIGO.");
            appUser.setMomoNetwork(net == null || net.isBlank() ? null : net.toUpperCase());
        }
        if ("buyer".equals(appUser.getRole())) {
            if (body.containsKey("businessName")) {
                String v = body.get("businessName");
                appUser.setBusinessName(v == null || v.isBlank() ? null : v.trim());
            }
            if (body.containsKey("goldbodLicenseNumber")) {
                String v = body.get("goldbodLicenseNumber");
                appUser.setGoldbodLicenseNumber(v == null || v.isBlank() ? null : v.trim());
            }
            if (body.containsKey("companyRegistrationNumber")) appUser.setCompanyRegistrationNumber(trim(body.get("companyRegistrationNumber")));
            if (body.containsKey("countryOfIncorporation")) appUser.setCountryOfIncorporation(trim(body.get("countryOfIncorporation")));
            if (body.containsKey("businessType")) appUser.setBusinessType(trim(body.get("businessType")));
            if (body.containsKey("positionTitle")) appUser.setPositionTitle(trim(body.get("positionTitle")));
            if (body.containsKey("exportLicenceNumber")) appUser.setExportLicenceNumber(trim(body.get("exportLicenceNumber")));
            if (body.containsKey("operatingJurisdiction")) appUser.setOperatingJurisdiction(trim(body.get("operatingJurisdiction")));
            if (body.containsKey("mineralsOfInterest")) appUser.setMineralsOfInterest(trim(body.get("mineralsOfInterest")));
            if (body.containsKey("typicalOrderVolume")) appUser.setTypicalOrderVolume(trim(body.get("typicalOrderVolume")));
            if (body.containsKey("preferredTransactionMethod")) appUser.setPreferredTransactionMethod(trim(body.get("preferredTransactionMethod")));
            if (body.containsKey("governmentIdDocument")) {
                String doc = body.get("governmentIdDocument");
                if (doc != null && doc.length() > 2_000_000)
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Document is too large.");
                appUser.setGovernmentIdDocument(doc);
            }
        }
        if ("government".equals(appUser.getRole())) {
            if (body.containsKey("regulatoryBodyName")) appUser.setRegulatoryBodyName(trim(body.get("regulatoryBodyName")));
            if (body.containsKey("officialIdBadgeNumber")) appUser.setOfficialIdBadgeNumber(trim(body.get("officialIdBadgeNumber")));
            if (body.containsKey("issuingAuthority")) appUser.setIssuingAuthority(trim(body.get("issuingAuthority")));
            if (body.containsKey("jurisdictionOfAuthority")) appUser.setJurisdictionOfAuthority(trim(body.get("jurisdictionOfAuthority")));
            if (body.containsKey("officialIdDocument")) {
                String doc = body.get("officialIdDocument");
                if (doc != null && doc.length() > 2_000_000)
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Document is too large.");
                appUser.setOfficialIdDocument(doc);
            }
        }
        if (body.containsKey("nationality")) appUser.setNationality(trim(body.get("nationality")));
        if (body.containsKey("dateOfBirth")) {
            String dobStr = body.get("dateOfBirth");
            if (dobStr == null || dobStr.isBlank()) {
                appUser.setDateOfBirth(null);
            } else {
                LocalDate dob;
                try {
                    dob = LocalDate.parse(dobStr.trim());
                } catch (Exception e) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "dateOfBirth must be in YYYY-MM-DD format.");
                }
                LocalDate today = LocalDate.now();
                if (dob.isAfter(today))
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Date of birth cannot be in the future.");
                if (dob.isAfter(today.minusYears(MINIMUM_WORKING_AGE)))
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Worker must be at least " + MINIMUM_WORKING_AGE + " years old (Ghana Labour Act, hazardous work).");
                appUser.setDateOfBirth(dob);
            }
        }
        if (body.containsKey("bloodType")) {
            String bt = body.get("bloodType");
            if (bt != null && !bt.isBlank()
                    && !java.util.Set.of("A+","A-","B+","B-","AB+","AB-","O+","O-","UNKNOWN").contains(bt.toUpperCase()))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "bloodType must be one of: A+, A-, B+, B-, AB+, AB-, O+, O-, Unknown.");
            appUser.setBloodType(bt == null || bt.isBlank() ? null : bt.toUpperCase());
        }
        if (body.containsKey("medicalNotes")) {
            String v = body.get("medicalNotes");
            appUser.setMedicalNotes(v == null || v.isBlank() ? null : v.trim());
        }
        if (body.containsKey("homeAddress")) {
            String v = body.get("homeAddress");
            if (v != null && v.length() > 500)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "homeAddress must be 500 characters or less.");
            appUser.setHomeAddress(v == null || v.isBlank() ? null : v.trim());
        }
        userRepo.save(appUser);
        return buildProfile(appUser);
    }

    private static final int MINIMUM_WORKING_AGE = 18;

    private static String trim(String v) { return v == null || v.isBlank() ? null : v.trim(); }

    @GetMapping("/api/profile/notification-preferences")
    @PreAuthorize("isAuthenticated()")
    public Map<String, Object> getNotificationPreferences(@AuthenticationPrincipal AuthenticatedUser user) {
        AppUser appUser = userRepo.findById(user.id())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("notifyHazard", !Boolean.FALSE.equals(appUser.getNotifyHazard()));
        m.put("notifyNotice", !Boolean.FALSE.equals(appUser.getNotifyNotice()));
        return m;
    }

    @org.springframework.web.bind.annotation.PutMapping("/api/profile/notification-preferences")
    @PreAuthorize("isAuthenticated()")
    public Map<String, Object> updateNotificationPreferences(
        @AuthenticationPrincipal AuthenticatedUser user,
        @RequestBody Map<String, Boolean> body
    ) {
        AppUser appUser = userRepo.findById(user.id())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (body.containsKey("notifyHazard")) appUser.setNotifyHazard(body.get("notifyHazard"));
        if (body.containsKey("notifyNotice")) appUser.setNotifyNotice(body.get("notifyNotice"));
        userRepo.save(appUser);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("notifyHazard", !Boolean.FALSE.equals(appUser.getNotifyHazard()));
        m.put("notifyNotice", !Boolean.FALSE.equals(appUser.getNotifyNotice()));
        return m;
    }

    @org.springframework.web.bind.annotation.PatchMapping("/api/profile/{email}/hr-details")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR','ROLE_SAFETY_OFFICER')")
    public Map<String, Object> updateHrDetails(
        @PathVariable String email,
        @AuthenticationPrincipal AuthenticatedUser user,
        @RequestBody Map<String, Object> body
    ) {
        AppUser worker = userRepo.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Worker not found"));
        if (user.assignedSite() == null || !user.assignedSite().equalsIgnoreCase(worker.getAssignedSite())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Worker is not on your site");
        }
        if (body.containsKey("jobTitle")) {
            Object v = body.get("jobTitle");
            worker.setJobTitle(v == null || v.toString().isBlank() ? null : v.toString().trim());
        }
        if (body.containsKey("employmentType")) {
            Object v = body.get("employmentType");
            String et = v == null ? null : v.toString().trim().toUpperCase();
            if (et != null && !et.isBlank()
                    && !java.util.Set.of("PERMANENT","CONTRACT","CASUAL").contains(et))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "employmentType must be PERMANENT, CONTRACT, or CASUAL.");
            worker.setEmploymentType(et == null || et.isBlank() ? null : et);
        }
        if (body.containsKey("nationalIdNumber")) {
            Object v = body.get("nationalIdNumber");
            worker.setNationalIdNumber(v == null || v.toString().isBlank() ? null : v.toString().trim());
        }
        if (body.containsKey("ssnitNumber")) {
            Object v = body.get("ssnitNumber");
            worker.setSsnitNumber(v == null || v.toString().isBlank() ? null : v.toString().trim());
        }
        if (body.containsKey("tinNumber")) {
            Object v = body.get("tinNumber");
            worker.setTinNumber(v == null || v.toString().isBlank() ? null : v.toString().trim());
        }
        if (body.containsKey("fitForDuty")) {
            Object v = body.get("fitForDuty");
            worker.setFitForDuty(v == null || !"false".equalsIgnoreCase(v.toString()));
        }
        userRepo.save(worker);
        return buildProfile(worker);
    }

    @GetMapping("/api/profile/{email}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR','ROLE_SAFETY_OFFICER')")
    public Map<String, Object> getWorkerProfile(
        @PathVariable String email,
        @AuthenticationPrincipal AuthenticatedUser user
    ) {
        AppUser worker = userRepo.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Worker not found"));
        if (user.assignedSite() == null || !user.assignedSite().equalsIgnoreCase(worker.getAssignedSite())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Worker is not on your site");
        }
        return buildProfile(worker);
    }

    private Map<String, Object> buildProfile(AppUser u) {
        long shiftCount = shiftLogRepo.countByWorkerEmailIgnoreCase(u.getEmail());
        long certCount = certRepo.countByWorkerEmailIgnoreCase(u.getEmail());
        long contactCount = ecRepo.findByWorkerIdOrderByContactTypeAsc(u.getId()).size();

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", u.getId());
        m.put("fullName", u.getFullName());
        m.put("email", u.getEmail());
        m.put("role", u.getRole());
        m.put("assignedSite", u.getAssignedSite());
        m.put("profilePhoto", u.getProfilePhoto());
        m.put("bio", u.getBio());
        m.put("createdAt", u.getCreatedAt());
        m.put("active", u.getActive());
        m.put("shiftLogCount", shiftCount);
        m.put("certificationCount", certCount);
        m.put("emergencyContactCount", contactCount);
        m.put("momoNumber", u.getMomoNumber());
        m.put("momoNetwork", u.getMomoNetwork());
        m.put("insuranceStatus", u.getInsuranceStatus() != null ? u.getInsuranceStatus() : "NOT_INSURED");
        m.put("nationality", u.getNationality());
        if ("buyer".equals(u.getRole())) {
            m.put("businessName", u.getBusinessName());
            m.put("goldbodLicenseNumber", u.getGoldbodLicenseNumber());
            m.put("buyerVerificationStatus", u.getBuyerVerificationStatus());
            m.put("companyRegistrationNumber", u.getCompanyRegistrationNumber());
            m.put("countryOfIncorporation", u.getCountryOfIncorporation());
            m.put("businessType", u.getBusinessType());
            m.put("positionTitle", u.getPositionTitle());
            m.put("exportLicenceNumber", u.getExportLicenceNumber());
            m.put("operatingJurisdiction", u.getOperatingJurisdiction());
            m.put("mineralsOfInterest", u.getMineralsOfInterest());
            m.put("typicalOrderVolume", u.getTypicalOrderVolume());
            m.put("preferredTransactionMethod", u.getPreferredTransactionMethod());
            m.put("ndaSigned", Boolean.TRUE.equals(u.getNdaSigned()));
        }
        if ("government".equals(u.getRole())) {
            m.put("regulatoryBodyName", u.getRegulatoryBodyName());
            m.put("officialIdBadgeNumber", u.getOfficialIdBadgeNumber());
            m.put("issuingAuthority", u.getIssuingAuthority());
            m.put("jurisdictionOfAuthority", u.getJurisdictionOfAuthority());
        }
        m.put("dateOfBirth", u.getDateOfBirth());
        m.put("bloodType", u.getBloodType());
        m.put("medicalNotes", u.getMedicalNotes());
        m.put("homeAddress", u.getHomeAddress());
        m.put("nationalIdNumber", u.getNationalIdNumber());
        m.put("ssnitNumber", u.getSsnitNumber());
        m.put("tinNumber", u.getTinNumber());
        m.put("jobTitle", u.getJobTitle());
        m.put("employmentType", u.getEmploymentType());
        m.put("fitForDuty", !Boolean.FALSE.equals(u.getFitForDuty()));
        return m;
    }
}
