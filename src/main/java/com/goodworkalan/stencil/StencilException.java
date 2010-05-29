package com.goodworkalan.stencil;

public class StencilException extends RuntimeException {
    /** The serial version id. */
    private static final long serialVersionUID = 1L;

    public StencilException(Throwable cause, String message, Object...arguments) {
        super(String.format(message, getClassName(arguments)), cause);
    }
    
    public StencilException(String message, Object...arguments) {
        super(String.format(message, getClassName(arguments)), null);
    }
    
    private static Object[] getClassName(Object...arguments) {
        for (int i = 0; i < arguments.length; i++) {
            if (arguments[i] instanceof Class<?>) {
                arguments[i] = ((Class<?>) arguments[i]).getName();
            }
        }
        return arguments;
    }
}
