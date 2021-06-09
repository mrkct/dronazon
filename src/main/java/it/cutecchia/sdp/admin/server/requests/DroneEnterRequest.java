package it.cutecchia.sdp.admin.server.requests;

import javax.xml.bind.annotation.XmlRootElement;

/** This object is used to represent a drone's request to enter the system. */
@XmlRootElement
public class DroneEnterRequest {
  private long droneId;

  public long getDroneId() {
    return droneId;
  }
}
