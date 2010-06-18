One aspect of this rewrite is get rid of all the <code>Operation</code>
objects and to simply iterate over a document in order, and generate an
output document based on the special Stencil elements and attributes
encoutered, rather than iterate over the document in order, create a bunch
of <code>Operation</code> objects for the Stencil elements and attributes
encountered, and have them generate an output document.

At the time, it must have seemed more object-oriented to write a bunch of
classes and instatiante a bunch of objects, instead of simply putting the
logic right in the, in any case unavoidable, if-else ladder.
