package it.cutecchia.sdp.admin.server.responses;

import com.google.gson.Gson;

public class JsonErrorResponse {
  private final String message;
  private final boolean success = false;

  public JsonErrorResponse(String message) {
    this.message = message;
  }

  public String getMessage() {
    return message;
  }

  public boolean getSuccess() {
    return success;
  }

  public static String create(Gson gson, String message) {
    return gson.toJson(new JsonErrorResponse(message));
  }
}
