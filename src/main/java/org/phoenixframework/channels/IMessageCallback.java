package qa.qserv.providers.networking.socket;


public interface IMessageCallback {

    /**
     * @param envelope The envelope containing the message payload and properties
     */
    void onMessage(final Envelope envelope);
}
