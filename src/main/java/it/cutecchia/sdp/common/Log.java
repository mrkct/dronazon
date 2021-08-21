package it.cutecchia.sdp.common;

public class Log {
  private static final String ANSI_RESET = "\u001B[0m";
  private static final String ANSI_CYAN = "\033[0;36m";
  private static final String ANSI_MAGENTA = "\033[0;35m";
  private static final String ANSI_BLUE = "\u001B[34m";
  private static final String ANSI_YELLOW = "\u001B[33m";
  private static final String ANSI_GREEN = "\033[32m";
  private static final String ANSI_RED = "\u001B[31m";

  private static final int LOG_LEVEL = 3;

  public static synchronized void info(String message, Object... args) {
    if (LOG_LEVEL < 5) return;
    synchronized (System.out) {
      synchronized (System.err) {
        System.out.printf("[INFO]: " + message + "%n" + ANSI_RESET, args);
      }
    }
  }

  public static synchronized void notice(String message, Object... args) {
    if (LOG_LEVEL < 4) return;
    synchronized (System.out) {
      synchronized (System.err) {
        System.out.printf(ANSI_CYAN + "[NOTICE]: " + message + "%n" + ANSI_RESET, args);
      }
    }
  }

  public static synchronized void warn(String message, Object... args) {
    if (LOG_LEVEL < 3) return;
    synchronized (System.out) {
      synchronized (System.err) {
        System.err.printf(ANSI_YELLOW + "[WARN]: " + message + "%n" + ANSI_RESET, args);
      }
    }
  }

  public static synchronized void error(String message, Object... args) {
    if (LOG_LEVEL < 2) return;
    synchronized (System.out) {
      synchronized (System.err) {
        System.err.printf(ANSI_RED + "[ERROR]: " + message + "%n" + ANSI_RESET, args);
      }
    }
  }

  public static synchronized void debug(String message, Object... args) {
    synchronized (System.out) {
      synchronized (System.err) {
        System.out.printf(ANSI_GREEN + "[DEBUG]: " + message + "%n" + ANSI_RESET, args);
      }
    }
  }

  public static synchronized void userMessage(String message, Object... args) {
    synchronized (System.out) {
      synchronized (System.err) {
        System.out.printf(ANSI_MAGENTA + message + "%n" + ANSI_RESET, args);
      }
    }
  }
}
