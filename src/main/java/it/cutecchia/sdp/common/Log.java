package it.cutecchia.sdp.common;

public class Log {
  private static final String ANSI_RESET = "\u001B[0m";
  private static final String ANSI_BLUE = "\u001B[34m";
  private static final String ANSI_YELLOW = "\u001B[33m";
  private static final String ANSI_RED = "\u001B[31m";

  public static synchronized void info(String message, Object... args) {
    synchronized (System.out) {
      System.out.printf(ANSI_RESET + "[INFO]: " + message + "%n", args);
    }
  }

  public static synchronized void notice(String message, Object... args) {
    synchronized (System.out) {
      System.out.printf(ANSI_BLUE + "[NOTICE]: " + message + "%n", args);
    }
  }

  public static synchronized void warn(String message, Object... args) {
    synchronized (System.err) {
      System.err.printf(ANSI_YELLOW + "[WARN]: " + message + "%n", args);
    }
  }

  public static synchronized void error(String message, Object... args) {
    synchronized (System.err) {
      System.err.printf(ANSI_RED + "[ERROR]: " + message + "%n", args);
    }
  }
}
