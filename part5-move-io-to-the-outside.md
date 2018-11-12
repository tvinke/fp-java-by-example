This is part 5 of the series called "Functional Java by Example". 

In previous part we stopped mutating our documents and returned copies of the data.

_If you came for the first time, it's best to start reading from the beginning. It helps to understand where we started and how we moved forward throughout the series._

These are all the parts:

* Part 1 - From Imperative to Declarative
* Part 2 - Tell a Story
* Part 3 - Don't Use Exceptions to Control Flow
* Part 4 - Prefer Immutability
* Part 5 - Move I/O to the Outside
* Part 6 - Functions as Parameters
* Part 7 - Treat Failures as Data Too
* Part 8 - More Pure Functions

I will update the links as each article is published. If you are reading this article through content syndication please check the original articles on [my blog](https://tedvinke.wordpress.com/tag/fp-java-by-example/).

Each time also the code is pushed to this [GitHub project](https://github.com/tvinke/fp-java-by-example). 

## Move I/O to the outside

Remember how we left things previously?

```groovy
class FeedHandler {

  Webservice webservice
  DocumentDb documentDb

  void handle(List<Doc> changes) {

    changes
      .findAll { doc -> isImportant(doc) }
      .each { doc ->
        createResource(doc)
        .thenAccept { resource ->
          documentDb.update(
            setToProcessed(doc, resource)
          )
        }
        .exceptionally { e ->
          documentDb.update(setToFailed(doc, e))
        }
      }
  }

  private CompletableFuture<Resource> createResource(doc) {
    webservice.create(doc)
  }
  
  private boolean isImportant(doc) {
    doc.type == 'important'
  }
  
  private Doc setToProcessed(doc, resource) {
    doc.copyWith(
      status: 'processed',
      apiId: resource.id
    )
  }
  
  private Doc setToFailed(doc, e) {
    doc.copyWith(
      status: 'failed',
      error: e.message
    )
  }

}
```

The example I'm evolving in each part of the series is some kind of "feed handler" which processes documents. 

What does the processing look like?

1. one or more documents come in
2. if a document is "important", it is saved to a webservice API which creates and returns a resource for it
3. if this succeeds, the document is marked as processed
4. if this fails, the document is marked as failed
5. ultimately, the document's updated in a database

The webservice could be a REST service (since we're talking about _resources_) and the database could be a document store as CouchDB or MongoDB (since we're talking about _documents_), but that doesn't really matter.

What matters is that there's some I/O (input/output) involved, usually in any system. Reading from the filesystem, loading en storing information in a database, communication across the network between webservices.

As we've seen in previous [instalment](https://tedvinke.wordpress.com/2018/06/15/functional-java-by-example-part-4-prefer-immutability/) we like our functions to be as _pure_ as possible, without any side-effects. Unfortunately real systems _have_ to interact with the outside world to be any meaningful.

How else would we get input into our system, or output anything to our users? Some examples of I/O are:

* file system access
* network sockets
* HTTP requests
* JDBC actions
* starting threads
* system clock access

We already got rid of our database access from our `setToProcessed`/`setToFailed` methods, by moving it one step up the call chain, but it's still inside the `FeedHandler`.

(image IO inside)

The best we can do is move I/O to the outside of the system.

The most obvious change we can do is to get rid of the DB altogether, and just return the new updated documents from `handle()`.

### Get rid of the database

Change
```groovy
.thenAccept { resource ->
  documentDb.update(
    setToProcessed(doc, resource)
  )
}
.exceptionally { e ->
  documentDb.update(setToFailed(doc, e))
}
```
to
```groovy
.thenApply { resource ->
  setToProcessed(doc, resource)
}
.exceptionally { e ->
  setToFailed(doc, e)
}
```
to get rid of `documentDb`. 

We're just returning any modified documents even further up the call chain. That's why we have also have to...

### ...get rid of void

Change the return type from
```groovy
void handle(...)
```
to
```groovy
List<Doc> handle(...)
```
so handled documents are returned all the way to the outside.

It's not that we don't have any interaction any more with any database, but that it's no longer a concern for our `FeedHandler` component! By moving any I/O to the outskirts of the system, everything in between can be as pure as possible.

(image io outside)

Remember Haskell, which is considered a "pure" functional language? From [Learn you a Haskell for Great Good](http://learnyouahaskell.com/input-and-output):

> It turns out that Haskell actually has a really clever system for dealing with functions that have side-effects that neatly separates the part of our program that is pure and the part of our program that is impure, which does all the dirty work like talking to the keyboard and the screen. With those two parts separated, we can still reason about our pure program and take advantage of all the things that purity offers, like laziness, robustness and modularity while efficiently communicating with the outside world.

When it was invented in the 90s, it introduced the `IO` monad to deal with I/O. Any function e.g. reading from the outside world _must_ use the return type `IO` which is actually being checked by the compiler.

This has a few benefits, such as that the Haskell compiler has some freedom in re-ordering all non-`IO` code for optimization. From [Pure Functions and I/O](https://alvinalexander.com/scala/fp-book/pure-functions-and-io-input-output):

> Because pure functional code is like algebra, the compiler can treat all non-IO functions as mathematical equations. This is somewhat similar to how a relational database optimizes your queries. 

In Java, we don't have such specific compiler support for these things, but there's a few things we can take care of ourselves.

Remember: `void` is a sink-hole. Any method returning `void` is either meaningless or operates through side-effects, such as writing to display, network, file or database - i.e. interaction with an external system. Instead of performing I/O as side-effect, return a value to the caller describing the interaction with the external system.

That's it for now!

_Just for giggles: [YouTube: Input/Output](https://www.youtube.com/watch?v=I5mwVv5NjhA)_