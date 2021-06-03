package tr.com.has.test;

import org.junit.Before;
import org.junit.Test;
import tr.com.has.SyncApi;
import tr.com.has.SyncConfig;
import tr.com.has.SyncServer;

import java.io.IOException;

import static org.junit.Assert.assertTrue;


public class SyncServerTest {

    @Before
    public void setup() throws IOException, InterruptedException {
        SyncConfig syncConfig = new SyncConfig("127.0.0.1", "127.0.0.1", 1453 , 1455);
        SyncServer syncServer = new SyncServer(syncConfig);
        syncServer.start();
    }

    @Test
    public void firstTest() throws IOException {
        SyncApi syncApi = new SyncApi(SyncConfig.DEFAULT_PORT);
        System.out.println(syncApi.amIActive());
        syncApi.waitForActive();

        assertTrue(syncApi.amIActive());
    }
}
