package net.aerh.terminalmonitor;

public class Terminal {
    
    private final int poiId;
    private final String name;
    private final String checkpointName;
    private final String floorId;
    private int queueTime;
    private boolean closed;
    
    public Terminal(int poiId, String checkpointName, String name, String floorId) {
        this.poiId = poiId;
        this.checkpointName = checkpointName;
        this.name = name;
        this.floorId = floorId;
    }
    
    public int getPoiId() {
        return poiId;
    }
    
    public String getCheckpointName() {
        return checkpointName;
    }
    
    public String getName() {
        return name;
    }
    
    public String getFloorId() {
        return floorId;
    }
    
    public int getQueueTime() {
        return queueTime;
    }
    
    public void setQueueTime(int queueTime) {
        this.queueTime = queueTime;
    }
    
    public boolean isClosed() {
        return closed;
    }
    
    public void setClosed(boolean closed) {
        this.closed = closed;
    }
}
