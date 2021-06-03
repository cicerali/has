package tr.com.has;

public interface SyncStatusCallback {
    void statusActive();
    void statusStandby();
    void statusUnknown();
    void mateUnAvailable();
    void mateAvailable();
    void audit(Status myStatus, Status mateStatus);
}
