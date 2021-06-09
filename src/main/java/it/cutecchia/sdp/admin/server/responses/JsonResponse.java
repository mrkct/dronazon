package it.cutecchia.sdp.admin.server.responses;

import com.google.gson.Gson;

public class JsonResponse<T> {
  private T data;
  private boolean success;
  private String message;

  private JsonResponse(boolean success, String message, T data) {
    this.success = success;
    this.message = message;
    this.data = data;
  }

  public boolean getSuccess() {
    return success;
  }

  public T getData() {
    return data;
  }

  public String getMessage() {
    return message;
  }

  public static <T> String success(Gson gson, T data) {
    return gson.toJson(new JsonResponse<>(true, null, data));
  }

  public static String error(Gson gson, String message) {
    return gson.toJson(new JsonResponse<>(false, message, null));
  }
}
