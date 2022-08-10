package com.gzoltar.gradle;
public class ExitException extends SecurityException {
  public ExitException(int status) {
    super("Caught call to System.exit("+status+")");
  }
}
