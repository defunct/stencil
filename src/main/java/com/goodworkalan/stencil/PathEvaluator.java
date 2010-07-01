package com.goodworkalan.stencil;

import com.goodworkalan.ilk.Ilk;
import com.goodworkalan.permeate.Path;

/**
 * Evaluates an entire path directly. 
 *
 * @author Alan Gutierrez
 */
public interface PathEvaluator<T> {
    /**
     * Evaluate the path against the boxed object.
     * 
     * @param box
     *            The box.
     * @param path
     *            The path.
     * @return The box.
     */
    public T evaluate(Ilk.Box box, Path path);
}
