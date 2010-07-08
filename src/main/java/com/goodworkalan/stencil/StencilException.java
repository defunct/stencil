package com.goodworkalan.stencil;

/**
 * An exception raised during Stencil evaluation.
 *
 * @author Alan Gutierrez
 */
public class StencilException extends RuntimeException {
    /** The serial version id. */
    private static final long serialVersionUID = 1L;

    /**
     * Create a Stencil exception with the given cause, using the given message
     * format and format arguments to create a message.
     * 
     * @param cause
     *            The cause.
     * @param message
     *            The message format.
     * @param arguments
     *            The message format arguments.
     */
    public StencilException(Throwable cause, String message, Object...arguments) {
        super(String.format(message, getClassName(arguments)), cause);
    }

    /**
     * Create a Stencil exception using the given message format and format
     * arguments to create a message.
     * 
     * @param message
     *            The message format.
     * @param arguments
     *            The message format arguments.
     */
    public StencilException(String message, Object...arguments) {
        super(String.format(message, getClassName(arguments)), null);
    }

    /**
     * Assert that the given exception is a reflection related
     * <code>Exception</code>. .
     * 
     * @param e
     *            The throwable.
     * @return The throwable.
     * @exception RuntimeException
     *                if the throwable is an <code>RuntimeException</code> but
     *                not an <code>IllegalArgumentException</code>.
     * @exception Error
     *                if the throwable is an <code>Error</code> but not an
     *                <code>ExceptionInInitializerError</code>.
     */
    public static Throwable $(Throwable e) {
        if (e instanceof Error) { 
            if (!(e instanceof ExceptionInInitializerError)) {
                throw (Error) e;
            }
        } else if (e instanceof RuntimeException) {
            if (!(e instanceof SecurityException) && !(e instanceof IllegalArgumentException)) {
                throw (RuntimeException) e;
            }
        }
        return e;
    }

    /**
     * Convert any classes in the format arguments to the value of their
     * <code>getName()</code> method since the default implementation of
     * <code>toString()</code> for classes annoys me.
     * 
     * @param arguments
     *            The format arguments.
     * @return The format arguments with any classes converted to their class
     *         names.
     */
    private static Object[] getClassName(Object...arguments) {
        for (int i = 0; i < arguments.length; i++) {
            if (arguments[i] instanceof Class<?>) {
                arguments[i] = ((Class<?>) arguments[i]).getName();
            }
        }
        return arguments;
    }
}
