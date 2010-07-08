package com.goodworkalan.stencil;

import java.lang.reflect.Type;
import java.util.Collection;

import com.goodworkalan.ilk.Ilk;

/**
 * A type collection for the item type of a collection iteration.
 * 
 * @author Alan Gutierrez
 *
 * @param <T> Type type
 */
class Actualizer<T> {
    /** The super type token of the collection item. */
    private Ilk<T> ilk;

    /** The type of collection item. */
    private final Type type;

    /**
     * Create a type collection using the given type.
     * 
     * @param type
     *            The type.
     */
    public Actualizer(Type type) {
        this.ilk = new Ilk<T>(){};
        this.type = type;
    }
    
    /**
     * Create a super type token with the actual type value assigned to the type
     * variable.
     * 
     * @return An actual super type token.
     */
    public Ilk<T> actual() {
        return ilk.assign(Actualizer.class.getTypeParameters()[0], type);
    }
    
    /**
     * Cast the given box to a collection of this item type.
     * 
     * @param box
     *            The box.
     * @return The contents of the box cast to a collection of this item type.
     */
    public Collection<T> collection(Ilk.Box box) {
        return box.cast(new Ilk<Collection<T>>(){}.assign(Actualizer.class.getTypeParameters()[0], type));
    }
}
