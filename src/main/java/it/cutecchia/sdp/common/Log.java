package it.cutecchia.sdp.common;

public class Log {
  public static synchronized void info(String message, Object... args) {
    System.out.printf("[INFO]: " + message + "%n", args);
  }

  public static synchronized void warn(String message, Object... args) {
    System.err.printf("[WARN]: " + message + "%n", args);
  }

  public static synchronized void error(String message, Object... args) {
    System.err.printf("[ERROR]: " + message + "%n", args);
  }
}
