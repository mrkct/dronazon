package it.cutecchia.sdp.drones;

import it.cutecchia.sdp.common.Order;

public interface OrderSource {
  interface OrderListener {
    void onOrderReceived(Order order);
  }

  void start(OrderListener listener);

  void stop();
}
