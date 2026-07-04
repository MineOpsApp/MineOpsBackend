package MineOpsBackend.service;

import MineOpsBackend.model.AppUser;

public interface InsuranceProviderService {
    void enroll(AppUser worker);
    boolean checkStatus(AppUser worker);
}
