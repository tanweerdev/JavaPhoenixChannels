package qa.qserv.providers.networking.socket;
//

import android.util.Log;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.json.JSONObject;

import qa.qserv.providers.BuildConfig;
import qa.qserv.providers.profile.domain.model.Notification;
import qa.qserv.providers.utils.Constants;
import qa.qserv.providers.utils.RxBus;
import qa.qserv.providers.utils.SessionManager;
import timber.log.Timber;

/*
 * Created by regbits on 12/14/17.
 */

public class QberSocket {

    private static QberSocket qberSocket;

    private Socket socket;

    private Channel locationUpdateChannel;

    private Channel scheduleChannel;

    private Channel pendingJobChannel;

    private long previousJobId;

    public static QberSocket getDefaultSocket() {
        if (qberSocket == null) {
            qberSocket = new QberSocket();
        }
        return qberSocket;
    }

    private Socket openConnection() throws Exception {
        if (socket == null) {
            String token = SessionManager.getToken();
            socket = new Socket(BuildConfig.SOCKET_URL + token);
            socket.connect();
        }
        return socket;
    }

    private Channel getLocationChannel(long jobId) throws Exception {

        if (jobId != this.previousJobId) {

            leaveLocationChannel();

        }
        this.previousJobId = jobId;
        if (locationUpdateChannel == null)
            locationUpdateChannel = openConnection().chan(Constants.CHANNEL_LOCATION + jobId, null);

        return locationUpdateChannel;

    }

    private Channel getScheduleChannel(long providerId) throws Exception {
        if (scheduleChannel == null)
            scheduleChannel = openConnection().chan(Constants.CHANNEL_SCHEDULE + providerId, null);

        return scheduleChannel;

    }


    private Channel getPendingJobChannel(long provider) throws Exception {

        if (pendingJobChannel == null)
            pendingJobChannel = openConnection().chan(Constants.CHANEL_PENDING_JOBS + provider, null);

        return pendingJobChannel;
    }


    public void joinPendingJobChannel(long providerId) throws Exception {
        final Channel channel = getPendingJobChannel(providerId);

//        final Channel channel = getScheduleChannel(providerId);
        channel.join().receive("ok", new IMessageCallback() {

            @Override
            public void onMessage(Envelope envelope) {
                channel.on(Constants.EVENT_CHANGE, new IMessageCallback() {
                    @Override
                    public void onMessage(Envelope envelope) {

                        if(envelope.getPayload().get("status_id").asText().equals("pending")) {
                            Notification notification = new Notification();
                            notification.setType(Notification.Type.NEW_JOB);
                            notification.setJobId(envelope.getPayload().get("id").asInt());
                            RxBus.defaultInstance().send(notification);
                        }
                    }
                });
            }
        });
    }

    private void joinLocationChannel(long jobId) throws Exception {
        getLocationChannel(jobId).join().receive("ok", new IMessageCallback() {

            @Override
            public void onMessage(Envelope envelope) {
                Timber.d("QberSocket joins  %s", envelope);
            }
        });
    }

    private void joinScheduleChannel(long providerId) throws Exception {
        final Channel channel = getScheduleChannel(providerId);
        channel.join().receive("ok", new IMessageCallback() {

            @Override
            public void onMessage(Envelope envelope) {
                channel.on(Constants.EVENT_CHANGE, new IMessageCallback() {
                    @Override
                    public void onMessage(Envelope envelope) {
                        ProviderSocketEvent event = new ProviderSocketEvent();
                        event.setEnvelope(envelope);
                        event.setEventType(ProviderSocketEvent.EVENT_SCHEDULE);
                        RxBus.defaultInstance().send(event);
                    }
                });
            }
        });


    }


    public void openPendingJobConnection(long providerId) throws Exception {
        if (providerId == 0) return;
        joinPendingJobChannel(providerId);
    }

    public void openScheduleConnection(long providerId) throws Exception {
        if (providerId == 0) return;
        joinScheduleChannel(providerId);
    }

    public void openLocationUpdateChannel(long jobId) throws Exception {
        if (jobId == 0) return;
        joinLocationChannel(jobId);


    }

    public void push(double latitude, double longitude) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("longitude", longitude);
            jsonObject.put("latitude", latitude);
            JSONObject locationObject = new JSONObject();
            locationObject.put("location", jsonObject);
            Timber.d(locationObject.toString());
            String eventName = "create:location";
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(locationObject.toString());
            locationUpdateChannel.push(eventName, node);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void closeConnection() {
        try {
            leaveLocationChannel();
            leaveScheduleChannel();
            leavePendingJobSchedule();
            if (socket != null) {
                socket.disconnect();
                socket = null;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void leaveLocationChannel() throws Exception {
        if (locationUpdateChannel != null) {
            locationUpdateChannel.leave();
            locationUpdateChannel = null;
        }
    }

    private void leaveScheduleChannel() throws Exception {
        if (scheduleChannel != null) {
            scheduleChannel.leave();
            scheduleChannel = null;
        }
    }

    public void leavePendingJobSchedule() throws Exception {
        if (pendingJobChannel != null) {
            pendingJobChannel.leave();
            pendingJobChannel = null;

        }
    }
}
