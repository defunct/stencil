package com.goodworkalan.stencil;

import java.lang.reflect.Type;
import java.util.Collection;

import com.goodworkalan.ilk.Ilk;

// TODO Document.
class Actualizer<T> {
    // TODO Document.
    private Ilk<T> ilk;
    // TODO Document.
    private final Type type;

    // TODO Document.
    public Actualizer(Type type) {
        this.ilk = new Ilk<T>(){};
        this.type = type;
    }
    
    // TODO Document.
    public Ilk<T> actual() {
        return ilk.assign(ilk, type);
    }
    
    // TODO Document.
    public Collection<T> collection(Ilk.Box box) {
        return box.cast(new Ilk<Collection<T>>(){}.assign(ilk, type));
    }
}
