package org.phoenixframework.channels;




public class ProviderSocketEvent {

    private Envelope envelope;
    private int eventType;
    public static int EVENT_SCHEDULE = -1111;
    public static int EVENT_EXPIRE_JOB = -1112;
    public static int EVENT_NEW_JOB = -1113;



    public Envelope getEnvelope() {
        return envelope;
    }

    public void setEnvelope(Envelope envelope) {
        this.envelope = envelope;
    }

    public int getEventType() {
        return eventType;
    }

    public void setEventType(int eventType) {
        this.eventType = eventType;
    }
}
