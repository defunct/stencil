package com.goodworkalan.stencil;

import java.util.Map;

/**
 * An escaper that replaces character codes with a mapped escape sequence.
 *
 * @author Alan Gutierrez
 */
public class CharCodeEscaper extends Escaper {
    /** The map of character code to escape sequences. */
    private final Map<Integer, String> escapes;
    
    /**
     * Create a character code lookup escaper using the given map of escapes.
     * 
     * @param escapes
     *            The map of escapes.
     */
    public CharCodeEscaper(Map<Integer, String> escapes) {
        this.escapes = escapes;
    }

    /**
     * Escape the given string by replacing characters with escape sequences.
     * 
     * @param unescaped
     *            The string to escape.
     * @return The escaped string.
     */
    public CharSequence escape(CharSequence unescaped) {
        StringBuilder escaped = new StringBuilder();
        for (int i = 0, stop = unescaped.length(); i < stop; i++) {
            char ch = unescaped.charAt(i);
            String sequence = escapes.get((int) ch);
            if (sequence != null) {
                escaped.append(sequence);
            } else {
                escaped.append(ch);
            }
        }
        return escaped.toString();
    }
}
