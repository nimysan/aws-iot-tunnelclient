package top.cuteworld.iotdemo;

public class HealthStatus {

    public HealthStatus(String thingId, int status) {
        this.thingId = thingId;
        this.status = status;
    }

    private String thingId;

    private int status;

    public String getThingId() {
        return thingId;
    }

    public void setThingId(String thingId) {
        this.thingId = thingId;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}
