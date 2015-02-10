package org.genepattern.gpunit;

/**
 * This is a proxy for a RuntimeException, meaning that an error occurred in the *implementation* of GpUnit
 * as opposed to an actual error thrown by one of the tests.
 * 
 * @author pcarr
 */
public class GpUnitException extends Exception {
    public GpUnitException() {
        super();
    }
    public GpUnitException(String message) {
        super(message);
    }
    public GpUnitException(Throwable t) {
        super(t);
    }
    public GpUnitException(String message, Throwable t) {
      super(message + " (" + t.getLocalizedMessage() + ")", t);
    }
}
