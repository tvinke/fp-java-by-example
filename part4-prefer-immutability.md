This is part 4 of the series called "Functional Java by Example". 

In previous part we talked a bit about _side effects_ and I'd like to elaborate a bit more about how we can prevent having our data manipulated in unexpected ways by **introducing _immutability_ into our code**.

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

## Pure functions

A small summary on what we discussed before.

* Functional Programming encourages side-effect free methods (or: functions), to make the code more **understandable and easier to reason about**. If a method just accepts certain input and returns the same output every time – which makes it a _pure_ function – all kinds of optimizations can happen under the hood e.g. by the compiler, or caching, parallelisation etc.

* We can replace _pure_ functions again by their (calculated) value, which is called [referential transparancy](https://en.wikipedia.org/wiki/Referential_transparency).

Here's what we currently have after the refactoring from the previous part:

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
          updateToProcessed(doc, resource)
        }
        .exceptionally { e ->
          updateToFailed(doc, e)
        }
      }
  }
  
  private CompletableFuture<Resource> createResource(doc) {
    webservice.create(doc)
  }
  
  private boolean isImportant(doc) {
    doc.type == 'important'
  }
  
  private void updateToProcessed(doc, resource) {
    doc.apiId = resource.id
    doc.status = 'processed'
    documentDb.update(doc)
  }
  
  private void updateToFailed(doc, e) {
    doc.status = 'failed'
    doc.error = e.message
    documentDb.update(doc)
  }
  
}
```
Our `updateToProcessed` and `updateToFailed` are "impure" -- they both update the existing document going _in_. As you can see by their return type, `void`, in Java this means: nothing comes _out_. A sink-hole.

```groovy  
private void updateToProcessed(doc, resource) {
  doc.apiId = resource.id
  doc.status = 'processed'
  documentDb.update(doc)
}

private void updateToFailed(doc, e) {
  doc.status = 'failed'
  doc.error = e.message
  documentDb.update(doc)
}
```

These kinds of methods are all around your typical code base. Consequently, as one's code base grows it tends to get harder to reason about the _state_ of the data after you've passed it to one of these methods.

Consider the following scenario:

```groovy
def newDocs = [
  new Doc(title: 'Groovy', status: 'new'),
  new Doc(title: 'Ruby', status: 'new')
]

feedHandler.handle(newDocs)

println "My new docs: " + newDocs
// My new docs: 
// [Doc(title: Groovy, status: processed),
//  Doc(title: Ruby, status: processed)]
// WHAT? My new documents aren't that 'new' anymore

```

Some culprit has been _mangling_ the status of my documents; first they're "new" and a second later they aren't; that's NOT ok! It must be that darn FeedHandler. Who authored that thing? _Why is it touching my data?!_ :-)

Consider another scenario, where there's more than one player handling your business.

```groovy
def favoriteDocs = [
  new Doc(title: 'Haskell'),
  new Doc(title: 'OCaml'),
  new Doc(title: 'Scala')
]

archiver.backup(favoriteDocs)

feedHandler.handle(favoriteDocs)

mangleService.update(favoriteDocs)

userDao.merge(favoriteDocs, true)

println "My favorites: " + favoriteDocs
// My favorites: []
// WHAT? Empty collection? Where are my favorites????
```

We start with a collection of items, and 4 methods later we find that our data is gone.

> In a world where everyone can mutate anything, it's hard to reason about any state at any given time. 

It's not even "global state" per se - a collection passed into a method can be cleared and variables can be changed by anyone who gets a hold of (a reference to) your data.

That's what _Joshua Bloch_ in [Effective Java](https://www.amazon.com/Effective-Java-Edition-Joshua-Bloch/dp/0321356683) (Item 15 _"Minimize Mutability"_ in the 2nd Edition and Item 13 _"Favor Immutability"_ in the 1st Edition) promotes:

> Classes should be immutable unless there's a very good reason to make them mutable. ...If a class cannot be made immutable, limit its mutability as much as possible.

## Prefer Immutability

So what is it? **An object is immutable if it does not change its state after it has been instantiated.**

Seems reasonable, right?

There's a ton of resources out there, about how to go about this in your particular language. Java, for instance, does not favor immutability by default; I have to do some work.

If there's a 3rd party which is making problems and changing data along the way (such as clearing my collection) one can quickly flush out the troublemaker by passing my collection in a _unmodifiable wrapper_ e.g.

```groovy
def data = [
  ...
]

// somewhere inside 3rd-party code
data.clear()

// back in my code:
// data is empty :-( 
```

Preventing trouble:

```groovy
def data = Collections
                 .unmodifiableCollection([])

// somewhere inside 3rd-party code
data.clear() // NOPE :-), throws UnsupportedOperationException
```

Inside your own code base we can prevent unintended side effect (such as my data being changed somewhere) **by minimizing mutable data structures**. In most FP languages like [Haskell](https://wiki.haskell.org/Functional_programming#Immutable_data), [OCaml](https://www2.lib.uchicago.edu/keith/ocaml-class/CLASS-1/immutability.html) and [Scala](https://docs.scala-lang.org/overviews/collections/overview.html) the language itself promotes _immutability by default_. While not really a FP language, writing [immutable JavaScript using ES6](https://wecodetheweb.com/2016/02/12/immutable-javascript-using-es6-and-beyond/) also tends to become good practice.

### Read-only first 

Using the principles we've learned so far, and drive to prevent unintended side effects, we want to make sure our `Doc` class **can not be changed** by anything after instantiating it - not even our `updateToProcessed`/`updateToFailed` methods.

This is our current class:
```groovy
class Doc {
  String title, type, apiId, status, error
}
```

Instead of doing all the manual labor of making a [Java class immutable](https://www.javaworld.com/article/2072958/immutable-java-objects.html), Groovy comes to the rescue with the `Immutable`-annotation. 

When put on the class, the Groovy compiler puts some enhancements in place, so NO ONE can update its state anymore after creation.

```groovy
@Immutable
class Doc {
  String title, type, apiId, status, error
}
```

The object becomes effectively "read-only" -- and any attempt to update a property will result in the aptly-named `ReadOnlyPropertyException` :-)

```groovy  
private void updateToProcessed(doc, resource) {
  doc.apiId = resource.id // BOOM! 
  // throws groovy.lang.ReadOnlyPropertyException: 
  //  Cannot set readonly property: apiId
  ...
}

private void updateToFailed(doc, e) {
  doc.status = 'failed' // BOOM! 
  // throws groovy.lang.ReadOnlyPropertyException: 
  //  Cannot set readonly property: status
  ...
}
```

_But wait, doesn't this mean that the `updateToProcessed`/`updateToFailed` methods will actually fail updating a document's `status` to "processed" or "failed"?_

Jip, that's what immutability brings us. How to repair the logic?

### Copy second

The [Haskell guide](https://wiki.haskell.org/Functional_programming#Immutable_data) on "Immutable data" gives us advice on how to proceed:

>  Purely functional programs typically operate on immutable data. Instead of altering existing values, altered copies are created and the original is preserved. Since the unchanged parts of the structure cannot be modified, they can often be shared between the old and new copies, which saves memory.

Answer: we clone it!

We do not _have_ to update the original data, we should make a copy of it -- the original is not ours and should be left untouched. Our `Immutable`-annotation supports this with a parameter, called `copyWith`.

```groovy
@Immutable(copyWith = true)
class Doc {
  String title, type, apiId, status, error
}
```

Consequently, we'll change our methods to **make a copy of the original with the altered status** (and api id and error message) -- and _return this copy_. 

_(The last statement in a Groovy method is always returned, doesn't need an explicit `return` keyword)_

```groovy
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
```

The database logic has also been moved up one level, taking the returned copy to store it.

We've gained control of our state!

This is it for now :-) 

_If you, as a Java programmer, worry about the performance implications of excessive object instantiation, there's a nice [reassuring post here](https://www.leadingagile.com/2018/03/immutability-in-java/)._

For reference, here's the full version of the refactored code.

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