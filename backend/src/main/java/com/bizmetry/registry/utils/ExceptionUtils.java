package com.bizmetry.registry.utils;

public final class ExceptionUtils {

  private ExceptionUtils() {
    // util class
  }

  /**
   * Devuelve la causa raíz (última en la cadena)
   */
  public static Throwable rootCause(Throwable t) {
    if (t == null) return null;

    Throwable cur = t;
    while (cur.getCause() != null && cur.getCause() != cur) {
      cur = cur.getCause();
    }
    return cur;
  }

  /**
   * Devuelve un mensaje usable incluso cuando getMessage() es null
   */
  public static String safeMessage(Throwable t) {
    if (t == null) return "";

    String msg = t.getMessage();
    if (msg != null && !msg.isBlank()) {
      return msg;
    }

    Throwable rc = rootCause(t);
    if (rc == null) return "";

    String rcMsg = rc.getMessage();
    if (rcMsg != null && !rcMsg.isBlank()) {
      return rc.getClass().getSimpleName() + ": " + rcMsg;
    }

    return rc.getClass().getSimpleName();
  }

  /**
   * Devuelve mensajes más humanos para errores comunes de red
   */
  public static String friendlyMessage(Throwable t) {
    Throwable rc = rootCause(t);
    if (rc == null) return safeMessage(t);

    // DNS / host inválido
    if (rc instanceof java.nio.channels.UnresolvedAddressException) {
      return "Unresolved host (invalid or not reachable DNS address)";
    }

    if (rc instanceof java.net.UnknownHostException) {
      return "Unknown host (DNS resolution failed)";
    }

    // Conexión
    if (rc instanceof java.net.ConnectException) {
      return "Connection failed (refused or unreachable)";
    }

    if (rc instanceof java.net.SocketTimeoutException) {
      return "Connection timed out";
    }

    return safeMessage(t);
  }

  /**
   * Devuelve una descripción técnica corta (útil para logs)
   */
  public static String technical(Throwable t) {
    Throwable rc = rootCause(t);
    if (rc == null) return "";

    String msg = rc.getMessage();
    return msg == null || msg.isBlank()
        ? rc.getClass().getName()
        : rc.getClass().getName() + ": " + msg;
  }
}
