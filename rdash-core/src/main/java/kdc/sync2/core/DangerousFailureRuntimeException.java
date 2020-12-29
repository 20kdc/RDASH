package kdc.sync2.core;

public class DangerousFailureRuntimeException extends RuntimeException {
    public DangerousFailureRuntimeException(String text, Exception t) {
        super(text, t);
    }
}
