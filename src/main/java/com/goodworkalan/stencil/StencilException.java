package com.goodworkalan.stencil;

// TODO Document.
public class StencilException extends RuntimeException {
    /** The serial version id. */
    private static final long serialVersionUID = 1L;

    // TODO Document.
    public StencilException(Throwable cause, String message, Object...arguments) {
        super(String.format(message, getClassName(arguments)), cause);
    }
    
    // TODO Document.
    public StencilException(String message, Object...arguments) {
        super(String.format(message, getClassName(arguments)), null);
    }
    
    // TODO Document.
    private static Object[] getClassName(Object...arguments) {
        for (int i = 0; i < arguments.length; i++) {
            if (arguments[i] instanceof Class<?>) {
                arguments[i] = ((Class<?>) arguments[i]).getName();
            }
        }
        return arguments;
    }
}
