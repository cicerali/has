package tr.com.has;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class SyncMessage implements Serializable {

    private int code;
    private Map<String, Object> map;

    public static final int HEARTBEAT = 0;
    public static final int STATUS_REQUEST = 1;
    public static final int STATUS_RESPONSE = 2;
    public static final int AM_I_ACTIVE_REQUEST = 3;
    public static final int AM_I_ACTIVE_RESPONSE = 4;
    public static final int WAIT_FOR_ACTIVE_REQUEST = 5;
    public static final int WAIT_FOR_ACTIVE_RESPONSE = 6;

    public SyncMessage() {
    }

    public SyncMessage(int code) {
        this.code = code;
        this.map = new HashMap<>();
    }

    public SyncMessage(int code, Map<String, Object> map) {
        this.code = code;
        this.map = map;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public Map<String, Object> getMap() {
        return map;
    }

    public void setMap(Map<String, Object> map) {
        this.map = map;
    }

    @Override
    public String toString() {
        return "SyncMessage{" +
                "mCode=" + code +
                ", sCode=" + codeString(code) +
                ", map=" + map +
                '}';
    }

    public static String codeString(int mCode) {

        String sCode;
        switch (mCode) {
            case HEARTBEAT: {
                sCode = "HEARTBEAT";
                break;
            }
            case STATUS_REQUEST: {
                sCode = "STATUS_REQUEST";
                break;
            }
            case STATUS_RESPONSE: {
                sCode = "STATUS_RESPONSE";
                break;
            }
            case AM_I_ACTIVE_REQUEST: {
                sCode = "AM_I_ACTIVE_REQUEST";
                break;
            }
            case AM_I_ACTIVE_RESPONSE: {
                sCode = "AM_I_ACTIVE_RESPONSE";
                break;
            }
            case WAIT_FOR_ACTIVE_REQUEST: {
                sCode = "WAIT_FOR_ACTIVE_REQUEST";
                break;
            }
            case WAIT_FOR_ACTIVE_RESPONSE: {
                sCode = "WAIT_FOR_ACTIVE_RESPONSE";
                break;
            }
            default: {
                sCode = "UNKNOWN(" + mCode + ")";
                break;
            }
        }
        return sCode;
    }
}
