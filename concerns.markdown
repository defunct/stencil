---
layout: default
title: Toughts, Concerns and Decisions regarding Stencil
---

# Thoughts on Stencil

 * Wondering if it wouldn't be nice to have a way to provide template to users,
   with a very simple markup language, around naked objects. Can Freemarker
   limit itself to use only specific classes that you specify?

   See: http://www.liquidmarkup.org/

It works, now with the basic utilities envisioned in the initial project.

## Navigating Maps

Not all types are going to be strongly typed. String Beans created a parallel
map that represents a diffused tree, so the permeate path language does not
quite work for it, it won't be type-safe.

And type safety always does fall apart with hetergenous types anyway.

It is a type error if the path does not resolve, you shouldn't ask for type that
does not exist, but that type information...

So, can you have a path mapped to a string? Assert that the path is there, then
get the string? Is it worth it? For JSON the untyped version is great. For
Stencil get a Path, associate the path itself with a string. That is type
correct but probably violates the spirit.

Can't fix this today. But, you've accepted that the misatkes need to be
rethought for different serialization strategies. Using `Permeate` paths as
keys, into a special map, with a definition of equality that goes soley by the
name and integerness of the index, would be a simple way to matain that safety.

However, now you have find a way to provide that compiled path, you might need a
special interface, one that take the path. This would also mean, you don't want
to use the path, if you don't have to.

Alternatively, you could make ad hoc navigation a fall back, which would be
simplier conceptually, but not "type-safe".

## Get with Local Bind

Might be really nice to have a one liner Bind and Get, or rather, an inline bind
and get is a necessity. Comma separated? Actually, Escape is less likely to
change in the course of a document.

@ Define and select.
@Escape(html => html.txt)

@ Select.
@Escape(html.txt)

@ Select.
@Escape(html)

@ Inline select.
@Escape(html)@Get(HttpServletRequest => contextPath html)@Escape!

## Import and Include

These will apply to the context of the parent element.

## Base URI

Is this necessary? Give a fully qualified URI and everything is resolved
relative to that. (Or do absolute URIs without schemes use the base URI?)
