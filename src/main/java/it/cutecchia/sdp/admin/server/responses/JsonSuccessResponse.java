package it.cutecchia.sdp.admin.server.responses;

import com.google.gson.Gson;

public class JsonSuccessResponse<T> {
  private final T data;
  private final boolean success = true;

  public JsonSuccessResponse(T data) {
    this.data = data;
  }

  public boolean getSuccess() {
    return success;
  }

  public T getData() {
    return data;
  }

  public static <T> String create(Gson gson, T data) {
    return gson.toJson(new JsonSuccessResponse<>(data));
  }
}
