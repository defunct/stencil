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
