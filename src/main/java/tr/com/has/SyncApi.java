package tr.com.has;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SyncApi {

    private static final Logger logger = Logger.getLogger(SyncApi.class.getName());

    private final InetSocketAddress syncServer;

    public SyncApi(int port) {
        this.syncServer = new InetSocketAddress("127.0.0.1", port);
    }

    public void waitForActive() throws IOException {
        SyncMessage syncMessage = new SyncMessage();
        syncMessage.setCode(SyncMessage.WAIT_FOR_ACTIVE_REQUEST);
        Status status = (Status) sendReceive(syncMessage, syncServer).getMap().getOrDefault(SyncServer.S_STATUS, Status.UNKNOWN);
        if (status != Status.ACTIVE) {
            throw new IOException("SyncServer error");
        }
    }

    public boolean amIActive() {
        SyncMessage syncMessage = new SyncMessage();
        SyncMessage response = new SyncMessage();
        syncMessage.setCode(SyncMessage.AM_I_ACTIVE_REQUEST);
        try {
            response = sendReceive(syncMessage, syncServer);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Communication error: {0}", e.getMessage());
            return false;
        }
        Status status = (Status) response.getMap().getOrDefault(SyncServer.S_STATUS, Status.UNKNOWN);
        return status == Status.ACTIVE;
    }

    private static SyncMessage sendReceive(SyncMessage syncMessage, InetSocketAddress dest) throws IOException {
        DatagramChannel channel = DatagramChannel.open();
        SyncServer.sendMessage(channel, syncMessage, dest);
        SyncMessage received = new SyncMessage();
        SyncServer.receiveMessage(channel, received);
        return received;
    }
}
