package com.goodworkalan.paste.validation;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.goodworkalan.ilk.Ilk;
import com.goodworkalan.okay.Mistakes;
import com.goodworkalan.permeate.Path;
import com.goodworkalan.stencil.PathEvaluator;
import com.goodworkalan.verbiage.Message;

/**
 * Evaluate a path against a mistakes map.
 *
 * @author Alan Gutierrez
 */
public class MistakesPathEvaluator implements PathEvaluator<List<Message>> {
    /** The mistakes. */
    private final Mistakes mistakes;
    /**
     * Create a path evaluator that obtains a collection of mistakes.
     * 
     * @param mistakes The mistakes.
     */
    @Inject
    public MistakesPathEvaluator(Mistakes mistakes) {
        this.mistakes = mistakes; 
    }

    /**
     * Get the mistakes for the given boxed object at the given path.
     * 
     * @param box
     *            The boxed binding context.
     * @param path
     *            The path.
     */
    public List<Message> evaluate(Ilk.Box box, Path path) {
        Map<String, List<Message>> fields = mistakes.mistakes.get(box.object); 
        if (fields != null) {
            return fields.get(path.get(0).getName());
        }
        return null;
    }
}
