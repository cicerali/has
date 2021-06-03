package tr.com.has;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SyncServer extends Thread {

    private static final Logger logger = Logger.getLogger(SyncServer.class.getName());

    private SyncConfig config;
    private SyncStatusCallback callback;

    private Status myStatus;
    private Status mateStatus;
    private boolean mateCheck = false;
    private int heartBeatMiss = 0;

    private List<InetSocketAddress> waitActiveList = new ArrayList<>();
    public static final String S_STATUS = "status";

    public SyncServer(SyncConfig config) {
        this.config = config;
        this.myStatus = Status.UNKNOWN;
        this.mateStatus = Status.UNKNOWN;
        super.setName("HA");
    }

    public void setCallback(SyncStatusCallback callback) {
        this.callback = callback;
    }

    @Override
    public void run() {

        InetSocketAddress mySocketAddress = new InetSocketAddress(config.getMyIp(), config.getMyPort());
        DatagramChannel channel = null;
        Selector selector = null;
        try {
            channel = DatagramChannel.open();
            channel.bind(mySocketAddress);
            channel.configureBlocking(false);
            selector = Selector.open();
            channel.register(selector, SelectionKey.OP_READ);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Unexpected error creating server channel: {0}", e.getMessage());
            System.exit(5);
        }

        DatagramChannel myChannel = channel;

        if (callback == null) {
            callback = new DefaultStatusCallback();
        }

        InetSocketAddress mateSocketAddress = new InetSocketAddress(config.getMateIp(), config.getMatePort());
        long auditTime = System.currentTimeMillis();
        while (true) {

            /* initial condition -> ask to mate */
            if (myStatus == Status.UNKNOWN) {
                SyncMessage message = new SyncMessage(SyncMessage.STATUS_REQUEST);
                logger.log(Level.FINE, "Trying to send STATUS_REQUEST syncMessage to mate: {0}", message);
                sendMessage(myChannel, message, mateSocketAddress);
            }

            /* wait for message or an audit*/
            try {
                selector.select(config.getAuditTimeout());
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Unexpected error during select: {0}", e.getMessage());
                continue;
            }

            Set<SelectionKey> readyKeys = selector.selectedKeys();
            if (readyKeys.isEmpty()) {
                logger.finest("Select timeout for timeout heartbeat");
            }

            /* if selected keys not empty this mean a sync message*/
            readyKeys.stream().filter(SelectionKey::isReadable).forEach(key -> {

                SyncMessage syncMessage = new SyncMessage();
                InetSocketAddress address = receiveMessage((DatagramChannel) key.channel(), syncMessage);

                /* message coming from mate: heartbeats or status messages */
                if (mateSocketAddress.equals(address)) {

                    /* mate sends heartbeat and it's status*/
                    if (syncMessage.getCode() == SyncMessage.HEARTBEAT) {
                        logger.log(Level.FINE, "HEARTBEAT syncMessage received");
                        heartBeatMiss = 0;
                        if (myStatus == Status.ACTIVE && syncMessage.getMap().get(S_STATUS).equals(Status.ACTIVE)) {
                            logger.log(Level.SEVERE, "Both of units thinks active, change local status to UNKNOWN");
                            myStatus = Status.UNKNOWN;
                            mateStatus = Status.UNKNOWN;
                            callback.statusUnknown();
                        }
                        if (!mateCheck) {
                            mateCheck = true;
                            callback.mateAvailable();
                        }
                    } else if (syncMessage.getCode() == SyncMessage.STATUS_REQUEST) { /* mate asks our status */
                        /* if our status assigned just send it to mate */
                        if (myStatus == Status.ACTIVE || myStatus == Status.STANDBY) {
                            syncMessage.setCode(SyncMessage.STATUS_RESPONSE);
                            syncMessage.getMap().clear();
                            syncMessage.getMap().put(S_STATUS, myStatus);
                            logger.log(Level.FINE, "Trying to send STATUS_RESPONSE syncMessage to mate: {0}", syncMessage);
                            sendMessage(myChannel, syncMessage, mateSocketAddress);
                        } else {
                            /* status not assigned yet, so tyr assign proper status
                             * If both unit not assigned its status will asks each other to their status
                             * it is decided according to ip-port pair, the unit will be active which have smaller ip address,
                             * if ip addresses equal then port will be checked */
                            logger.log(Level.FINE, "Mate ip: {0}, my ip: {1}", new Object[]{address.getAddress().getHostAddress(), mySocketAddress.getAddress().getHostAddress()});
                            if (mySocketAddress.getAddress().getHostAddress().compareTo(address.getAddress().getHostAddress()) <= 0) {
                                if (config.getMyPort() < config.getMatePort()) {
                                    logger.log(Level.INFO, "This unit will be active");
                                    myStatus = Status.ACTIVE;
                                    mateStatus = Status.STANDBY;
                                    callback.statusActive();
                                    waitActiveList.forEach(dest -> {
                                        syncMessage.setCode(SyncMessage.WAIT_FOR_ACTIVE_RESPONSE);
                                        syncMessage.getMap().clear();
                                        syncMessage.getMap().put(S_STATUS, myStatus);
                                        sendMessage(myChannel, syncMessage, dest);
                                    });
                                    waitActiveList.clear();
                                } else if (config.getMyPort() > config.getMatePort()) {
                                    logger.log(Level.INFO, "Mate unit will be active");
                                    myStatus = Status.STANDBY;
                                    mateStatus = Status.ACTIVE;
                                    callback.statusStandby();
                                }
                            } else {
                                logger.log(Level.INFO, "Mate unit will be active");
                                myStatus = Status.STANDBY;
                                mateStatus = Status.ACTIVE;
                                callback.statusStandby();
                            }
                        }
                    } else if (syncMessage.getCode() == SyncMessage.STATUS_RESPONSE) { /* mate response our status request */
                        logger.fine("Status response message received");
                        if (syncMessage.getMap().get(S_STATUS).equals(Status.ACTIVE)) {
                            logger.log(Level.INFO, "This unit will be standby");
                            mateStatus = Status.ACTIVE;
                            myStatus = Status.STANDBY;
                            callback.statusStandby();
                        } else if (syncMessage.getMap().get(S_STATUS).equals(Status.STANDBY)) {
                            logger.log(Level.INFO, "This unit will be active");
                            mateStatus = Status.STANDBY;
                            myStatus = Status.ACTIVE;
                            waitActiveList.forEach(dest -> {
                                syncMessage.setCode(SyncMessage.WAIT_FOR_ACTIVE_RESPONSE);
                                syncMessage.getMap().clear();
                                syncMessage.getMap().put(S_STATUS, myStatus);
                                sendMessage(myChannel, syncMessage, dest);
                            });
                            waitActiveList.clear();
                            callback.statusActive();
                        } else {
                            logger.log(Level.SEVERE, "Unexpected condition");
                        }
                    }
                } else if (syncMessage.getCode() == SyncMessage.AM_I_ACTIVE_REQUEST) { /* application asks it's status */
                    syncMessage.setCode(SyncMessage.AM_I_ACTIVE_RESPONSE);
                    syncMessage.setMap(new HashMap<>());
                    syncMessage.getMap().put(S_STATUS, myStatus);
                    sendMessage(myChannel, syncMessage, address);
                } else if (syncMessage.getCode() == SyncMessage.WAIT_FOR_ACTIVE_REQUEST) { /* application will wait to active status */
                    if (myStatus == Status.ACTIVE) {
                        syncMessage.setCode(SyncMessage.WAIT_FOR_ACTIVE_RESPONSE);
                        syncMessage.setMap(new HashMap<>());
                        syncMessage.getMap().put(S_STATUS, myStatus);
                        sendMessage(myChannel, syncMessage, address);
                    } else {
                        waitActiveList.add(address);
                    }
                } else {
                    logger.log(Level.SEVERE, "Unexpected message code received");
                }
            });
            readyKeys.clear();

            long currentTime = System.currentTimeMillis();
            /* audit and heartbeat */
            if (auditTime + config.getAuditTimeout() < currentTime) {
                auditTime = currentTime;
                SyncMessage syncMessage = new SyncMessage();
                syncMessage.setCode(SyncMessage.HEARTBEAT);
                syncMessage.setMap(new HashMap<>());
                heartBeatMiss++;

                /* if 5 heartbeat message missing then this unit will think mate gone and sets current status to active */
                if (heartBeatMiss >= 5) {
                    if (myStatus != Status.ACTIVE) {
                        myStatus = Status.ACTIVE;
                        mateStatus = Status.UNKNOWN;
                        syncMessage.getMap().clear();
                        syncMessage.getMap().put(S_STATUS, myStatus);
                        logger.log(Level.INFO, "Lots of missing heartbeat syncMessage, changing status to active");
                        waitActiveList.forEach(dest -> {
                            syncMessage.setCode(SyncMessage.WAIT_FOR_ACTIVE_RESPONSE);
                            syncMessage.getMap().clear();
                            syncMessage.getMap().put(S_STATUS, myStatus);
                            sendMessage(myChannel, syncMessage, dest);
                        });
                        waitActiveList.clear();
                        callback.statusActive();
                        mateCheck = true;
                    }

                    if (mateCheck) {
                        mateCheck = false;
                        callback.mateUnAvailable();
                    }
                }

                /* */
                callback.audit(myStatus, mateStatus);
                logger.log(Level.FINE, "Trying to send HEARTBEAT syncMessage to mate: {0}", syncMessage);
                sendMessage(myChannel, syncMessage, mateSocketAddress);
            }
        }
    }

    public static boolean sendMessage(DatagramChannel srcChannel, SyncMessage message, InetSocketAddress dstAddress) {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        try {
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteOut);
            objectOutputStream.writeObject(message);
            objectOutputStream.flush();
            byte[] temp = byteOut.toByteArray();
            objectOutputStream.close();
            ByteBuffer buffer = ByteBuffer.allocate(temp.length);
            buffer.put(temp);
            buffer.flip();
            srcChannel.send(buffer, dstAddress);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Unexpected error during message send: {0}", e.getMessage());
            return false;
        }
        logger.log(Level.FINEST, "Message successfully send: {0}", message);
        return true;
    }

    public static InetSocketAddress receiveMessage(DatagramChannel channel, SyncMessage syncMessage) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(512);
        InetSocketAddress remoteAddress = null;
        try {
            remoteAddress = (InetSocketAddress) channel.receive(byteBuffer);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Unexpected error during message receive: {0}", e.getMessage());
        }
        int pos = byteBuffer.position();
        if (pos > 0) {
            byteBuffer.flip();
            ByteArrayInputStream bis = new ByteArrayInputStream(byteBuffer.array());
            try {
                ObjectInput in = new ObjectInputStream(bis);
                SyncMessage o = (SyncMessage) in.readObject();
                logger.log(Level.FINE, "Received syncMessage: {0}, from: {1}", new Object[]{o, remoteAddress});
                syncMessage.setCode(o.getCode());
                syncMessage.setMap(o.getMap());
                return remoteAddress;
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Unexpected error during message read: {0}", e.getMessage());
            }
        }
        return null;
    }
}
