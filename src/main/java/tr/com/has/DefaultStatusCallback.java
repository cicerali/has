package tr.com.has;

import java.util.logging.Level;
import java.util.logging.Logger;

public class DefaultStatusCallback implements SyncStatusCallback {

    private final Logger logger = Logger.getLogger(DefaultStatusCallback.class.getName());

    @Override
    public void statusActive() {
        logger.log(Level.FINE, "status: Active");
    }

    @Override
    public void statusStandby() {
        logger.log(Level.FINE, "status: Standby");
    }

    @Override
    public void statusUnknown() {
        logger.log(Level.FINE, "status: Unknown");
    }

    @Override
    public void mateUnAvailable() {
        logger.log(Level.FINE, "status: mateUnAvailable");
    }

    @Override
    public void mateAvailable() {
        logger.log(Level.FINE, "status: mateAvailable");
    }

    @Override
    public void audit(Status myStatus, Status mateStatus) {
        logger.log(Level.FINE, "myStatus: {0}, mateStatus {1}", new Object[]{myStatus, mateStatus});
    }
}
