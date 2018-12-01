This is part 6 of the series called "Functional Java by Example". 

The example I'm evolving in each part of the series is some kind of "feed handler" which processes documents. In previous part we tried to make our functions as _pure_ possible by moving as much of the side-effects, such as IO, to the outside of the system.

Now we're going to replace some of our abstractions into functions, to be passed as parameters.

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

## Collaborators OO-style

Remember how we left things previously?

```groovy
class FeedHandler {

  Webservice webservice

  List<Doc> handle(List<Doc> changes) {

    changes
      .findAll { doc -> isImportant(doc) }
      .collect { doc ->
        createResource(doc)
        .thenApply { resource ->
          setToProcessed(doc, resource)
        }
        .exceptionally { e ->
          setToFailed(doc, e)
        }
        .get()
      }
  }

  private CompletableFuture<Resource> createResource(doc) {
    webservice.create(doc)
  }
  
  private static boolean isImportant(doc) {
    doc.type == 'important'
  }
  
  private static Doc setToProcessed(doc, resource) {
    doc.copyWith(
      status: 'processed',
      apiId: resource.id
    )
  }
  
  private static Doc setToFailed(doc, e) {
    doc.copyWith(
      status: 'failed',
      error: e.message
    )
  }

}
```

Above feed handler needs a "web service" to do its work. 

Take a look at the following part where a collaborator of type `WebService` is used to create a resource based on a document:

```groovy
class FeedHandler {

  Webservice webservice

  List<Doc> handle(List<Doc> changes) {

    changes
      .collect { doc ->
        createResource(doc)
        ...
  }

  private CompletableFuture<Resource> createResource(doc) {
    webservice.create(doc)
  }
  
}
```

_Remember, instead of just returning a resource directly, we've wrapped it in a `CompletableFuture` [as part of our exception handling mechanism](https://tedvinke.wordpress.com/2018/01/19/functional-java-by-example-part-3-dont-use-exceptions-to-control-flow/)._

**What if we wanted something other than a `WebService` to create a resource?**

Well, this is where it gets tricky and easy at the same time -- and where a OO-style can conflict a bit with a FP-style.

You see, `WebService` is a Java interface and defined as follows:

```groovy
interface Webservice {
  CompletableFuture<Resource> create(Doc doc)
}
```

This follows the _Dependency Inversion Principle (DIP)_ -- as part of the SOLID design principles promoted by [Robert C. Martin](http://blog.cleancoder.com/) -- which (amongst others) says:

> Abstractions should not depend on details. Details should depend on abstractions.

`WebService` is already an abstraction for any kind of webservice _implementation_. So the system could have multiple implementations of this interface e.g. a REST implementation and a SOAP implementation:

```groovy
class RestWebService implements Webservice {
  @Override
  CompletableFuture<Resource> create(Doc doc) {
    // do REST communication
  }
}
class SoapWebService implements Webservice {
  @Override
  CompletableFuture<Resource> create(Doc doc) {
    // do SOAP communication
  }
}
```

The feed handler does not care about the _details_ -- it just wants something which adheres to the contract defined by the `WebService` interface: there's a `create` method which accepts a `Doc` and returns a `CompletableFuture<Resource>`.

The `FeedHandler` class has a property `webservice` holding the reference to a `WebService`. Any OO-developer recognizes this style, because it's very familiar: all the collaborators are present in properties, which are (often) initialized during constructing. 

As soon as `FeedHandler` is constructed, it gets an instance of `WebService` passed to it - albeit constructor-injection or property-injection, either through DI frameworks or plain-old manual labor.

_For brevity I have been omitting the constructor in my code snippets, but as you can see in [my testcases](https://github.com/tvinke/fp-java-by-example/tree/master/src/test/groovy) I definitely pass all dependencies using the constructor Groovy generates for me under the hood :-)_

## Collaborators FP-style

Ok, if we would put on our Functional Hat again, we would need to revisit the way how a `WebService` gets passed to the feed handler.

The `handle` method's signature does not mention anything other than: documents go _in_, and documents come _out_. 

```groovy
class FeedHandler {

  ...

  List<Doc> handle(List<Doc> changes) {

    ...
  }


}
```

I can not assume the _same output_ is returned for the _same input_ -- because the method secretly depends on something on the outside: the `WebService`. 

_Well, possibly I control the entire creation of the feed handler, including the `WebService`, but the reference to `webservice` can change in between method invocations, yielding other results every time `handle` is using it. Unless I made it [immutable](https://tedvinke.wordpress.com/2018/06/15/functional-java-by-example-part-4-prefer-immutability/) or prevent the reference from being updated. I told you it could get tricky ;-)_

Can we make `handle` _pure_, just as we did in previous installments with the `isImportant`, `setToProcessed` and `setToFailed` methods?

In this case we have to pass `WebService` in as a **parameter**, just as the list of documents.

We change
```groovy
class FeedHandler {

  Webservice webservice

  List<Doc> handle(List<Doc> changes) {

    ...
  }

}
```

into

```groovy
class FeedHandler {

  List<Doc> handle(List<Doc> changes, Webservice webservice) {

    ...
  }

}
```

At every invocation of `handle` we pass in everything it needs: the documents it needs to handle and the webservice it needs to use. 

_Since this method no longer depends on any properties in the `FeedHandler` class anymore, we could have made it `static` at the moment -- upgrading it to a class-level method._

## Higher-order functions

Effectively our `handle` method just became a so-called "higher order function", a function that takes a function or returns a function.

So, back to a question I asked in the beginning: _what if we wanted something other than a `WebService` to create a resource?_ 

It shouldn't even have to be a webservice right? Maybe we completely want to go bananas and a have a monkey create a resource for us?

```groovy
class Monkey implements Webservice {
  @Override
  CompletableFuture<Resource> create(Doc doc) {
    // go bananas! But do create resources plz
  }
}
```

That just looks weird, doesn't it? The `WebService` interface is _too specific_ for the abstraction feed handler needs. Anything which _creates_ resources will do, doesn't it?

A better name would be _"ResourceCreator"_ -- so just rename the interface.

Old:
```groovy
interface Webservice {
  CompletableFuture<Resource> create(Doc doc)
}
```

New:
```groovy
interface ResourceCreator {
  CompletableFuture<Resource> create(Doc doc)
}
```

A `ResourceCreator` interface with a `create` method; how fitting! Now anything can implement this interface, and feed handler doesn't even care whether or not it is a webservice, a monkey or a Hobbit.

The new method signature:
```groovy
class FeedHandler {

  List<Doc> handle(List<Doc> changes, ResourceCreator creator) {

    ...
  }

}
```

Perfect abstraction!

## Functional abstractions

In Java we call an interface with only _one abstract method_ a **functional interface**. Our `ResourceCreator` fits this description; it has a single, abstract method `create`.

Java's [java.util.function](https://docs.oracle.com/javase/8/docs/api/java/util/function/package-summary.html) package has numerous of those functional interfaces -- and they each have a single, defined purpose:

* `Consumer` represents a function that accepts an argument and returns nothing
* `Supplier` represents a function that accepts no arguments, just returns a result
* **`Function` represents a function that accepts one argument and returns a result**
* ...and more

What this means is, that we don't need to define a specific interface, such as `ResourceCreator`, every time we need a function "to accept one argument and return a result" -- `Function` is already an interface we can leverage!

This is how `Function` (simplified) in Java 8 looks like:
```groovy
interface Function<T,R> {
  R apply(T t);
}
```
And this is how `ResourceCreator` looks like right now:
```groovy
interface ResourceCreator {
  CompletableFuture<Resource> create(Doc doc)
}
```

You see we can completely substitute our `ResourceCreator` with a `Function` if we:
* substitute `Doc` for type `R`
* substitute `CompletableFuture<Resource>` for type `T`
* substitute calling `create` by the method `apply`

We can erase the `ResourceCreator` interface completely! 

The new method signature will become:
```groovy
class FeedHandler {

  List<Doc> handle(List<Doc> changes,
      Function<Doc, CompletableFuture<Resource>> creator) {

    ...
  }

}
```

What have we achieved?

* We can pass any _function_ to `handle` now which takes a single `Doc` and produces a single `CompletableFuture<Resource>` -- and that's all the feed handler needs to work properly.
* As you've probably noticed by now that Functional Programming deals a lot with _functions_. A function can take another function, or could return a function.
* As of Java 8 we've got a whole bunch of functional interfaces, ready to use. Every developer can work with them in a standardized way, so it's best to see if they fit your use case and API and re-use them wherever possible. Every one of them have generic types (such as `T` and `R`) which can be used by you to indicate what goes _in_ and what comes _out_ of a function. 

The complete code now looks like this:
```groovy
class FeedHandler {

  List<Doc> handle(List<Doc> changes,
    Function<Doc, CompletableFuture<Resource>> creator) {

    changes
      .findAll { doc -> isImportant(doc) }
      .collect { doc ->
        creator.apply(doc)
        .thenApply { resource ->
          setToProcessed(doc, resource)
        }
        .exceptionally { e ->
          setToFailed(doc, e)
        }
        .get()
      }
  }
  
  private static boolean isImportant(doc) {
    doc.type == 'important'
  }
  
  private static Doc setToProcessed(doc, resource) {
    doc.copyWith(
      status: 'processed',
      apiId: resource.id
    )
  }
  
  private static Doc setToFailed(doc, e) {
    doc.copyWith(
      status: 'failed',
      error: e.message
    )
  }

}
```

That's it for now!  Next time, we're going to treat failures a data.

If you have any comments or suggestions, I'd love to hear about them.