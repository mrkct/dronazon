package it.cutecchia.sdp.dronazon;

import it.cutecchia.sdp.common.Order;

import java.util.logging.Logger;

public class MQTTOrderPublisher implements OrderPublisher {
    private static final Logger log = Logger.getLogger(MQTTOrderPublisher.class.getName());


    public MQTTOrderPublisher(String mqttServer, String topic) {
        // TODO: Attempt to connect to mqtt server
    }

    @Override
    public void publishOrder(Order order) {
        // TODO: Send data to topic
        log.info(String.format("Sending order %s", order.toString()));
    }
}
