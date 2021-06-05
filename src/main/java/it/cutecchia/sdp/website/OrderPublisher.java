package it.cutecchia.sdp.website;

import it.cutecchia.sdp.common.Order;

public interface OrderPublisher {
  void publishOrder(Order order);
}
