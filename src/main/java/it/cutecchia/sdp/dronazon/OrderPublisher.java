package it.cutecchia.sdp.dronazon;

import it.cutecchia.sdp.common.Order;

public interface OrderPublisher {
    void publishOrder(Order order);
}
