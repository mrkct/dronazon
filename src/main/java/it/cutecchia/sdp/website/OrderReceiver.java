package it.cutecchia.sdp.website;

import it.cutecchia.sdp.common.Order;

public abstract class OrderReceiver {
  interface OrderReceivedListener {
    void onOrderReceived(Order order);
  }

  private OrderReceivedListener listener = null;

  public void setOnOrderReceivedListener(OrderReceivedListener listener) {
    this.listener = listener;
  }

  protected void notifyListeners(Order order) {
    if (this.listener != null) {
      this.listener.onOrderReceived(order);
    }
  }
}
