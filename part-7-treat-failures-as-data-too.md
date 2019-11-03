This is part 7 of the series called "Functional Java by Example". 

The example I'm evolving in each part of the series is some kind of "feed handler" which processes documents. We've already dealt with exceptional situations before, but we're going to take care of them, more ehm,...functionally -- as _data_.

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

## Failing gracefully: small recap

This is how we left things previously:

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

Above feed handler's primary responsibility is to "handle" a list of changed documents, which seems to be to create every time a "resource" out of a document and process it further.

This has been abstracted in previous part to a function which accepts a `Doc` and returns a `Resource`, which in Java looks like: `Function<Doc, CompletableFuture<Resource>> creator`

You can see the resource is actually wrapped in a `CompletableFuture` (CF), which allows us to _chain_ method calls, such as `thenApply` and `exceptionally`. In [part 3 (Don’t Use Exceptions to Control Flow)](https://tedvinke.wordpress.com/2018/01/19/functional-java-by-example-part-3-dont-use-exceptions-to-control-flow/) we introduced `exceptionally` to replace the part where we used `try-catch` to deal with a possible exception when creating a resource.

The code at the time looked like:
```groovy
try {
  def resource = createResource(doc)
  updateToProcessed(doc, resource)
} catch (e) {
  updateToFailed(doc, e)
}
```

We replaced it with:

```groovy
createResource(doc)
.thenAccept { resource ->
  updateToProcessed(doc, resource)
}.exceptionally { e ->
  updateToFailed(doc, e)
}
```

The CF allowed us to signal _"exceptional" completion_ without using side-effects such as throwing an `Exception`. In the Java SDK this one of the few classes that encapsulates a result (success or failure) and shares monadic properties with e.g. an `Optional` (present or empty value).

In other languages such a Scala there's a dedicated type for this, called a `Try`.

## Try

From the [Scala Try](https://www.scala-lang.org/api/current/scala/util/Try.html) docs:

> The Try type represents a computation that may either result in an exception, or return a successfully computed value.

Scala developers which use `Try` would not need to do explicit exception handling everywhere an exception might occur. What if we were to use it in Java too?

Fortunately, there's a library called [Vavr](https://www.vavr.io/) which contains a whole lot of functional utilities we can use in our Java projects.

Example from the [Vavr Try](https://www.vavr.io/vavr-docs/#_try) docs shows us how easy it is to forget about exceptions completely:

```java
Try.of(() -> bunchOfWork()).getOrElse(other);
```

We either get the result from `bunchOfWork()` upon success, or `other` in case of failures along the way.

This class is actually an interface and has a whole bunch of default methods which all return the instance itself, which allows to chain _ad infinitum_, such as:

* `andFinally` - Provides try's finally behavior no matter what the result of the operation is.
* `andThen` - Runs the given runnable if this is a Success, otherwise returns this Failure.
* `filter` - Returns this if this is a Failure or this is a Success and the value satisfies the predicate.
* `onFailure` - Consumes the throwable if this is a Failure.
* `onSuccess` - Consumes the value if this is a Success.
* `map` - Runs the given checked function if this is a Success, passing the result of the current expression to it.

Methods which return an ultimate value:

* `get` - Gets the result of this Try if this is a Success or throws if this is a Failure.
* `getCause` - Gets the cause if this is a Failure or throws if this is a Success.
* `getOrElse` - Returns the underlying value if present, otherwise another value.
* `getOrElseGet` - Returns the underlying value if present, otherwise a value from another Function.
* `getOrElseThrow` - Returns the underlying value if present, otherwise throws supplier.get().
* `getOrElseTry` - Returns the underlying value if present, otherwise returns the result of Try.of(supplier).get().
* `getOrNull` - Returns the underlying value if present, otherwise `null`.

How can our code benefit after we've included the library in our project?

**Just replace our `CompletableFuture<Resource>` with `Try<Resource>`.**

Consequently, replace our calls to `thenApply/exceptionally` to `map/getOrElseGet`'

```groovy
creator.apply(doc)
.thenApply { resource ->
  // ...
}.exceptionally { e ->
  // ...
}.get()

```

becomes

```groovy
creator.apply(doc)
.map { resource ->
  // ...
}.getOrElseGet { e ->
  // ...
}
```

The Try's `map`-method accepts a function which runs when the try is a 'success' (as before). The `getOrElseGet`-method accepts a function in case of a failure e.g. an exception (as before).

You could peek inside, just as with a `Stream`, e.g.
```groovy
creator.apply(doc)
.peek { resource ->
  println "We've got a $resource"
}
.map { resource ->
  // ...
}.getOrElseGet { e ->
  // ...
}
```

Or you could add some more logging for development- or troubleshooting purposes e.g.

```groovy
creator.apply(doc)
.peek { resource ->
  println "We've got a $resource"
}.onSuccess { resource ->
  println "Successfully created $resource"
}.onFailure { e ->
  println "Bugger! Got a $e"
}.map { resource ->
  // ...
}.onSuccess { document ->
  println "Successfully processed $document"
}.onFailure { e ->
  println "Bugger! Processing failed with $e"
}.getOrElseGet { e ->
  // ...
}
```

On the surface it seems nothing has changed much. It's just replacing one set of method calls to some others, and in this case that's all there's too it :-) 

However, you may choose `Try` over a `CompletableFuture` because it might seem a more natural fit for what we want to achieve -- there's nothing "futuristic" about our computation, there's nothing to schedule or become available "at some point in time".

But there's more.

## Recover from failure

What we've got now, is that if the resource creator API fails, any failure is nicely wrapped in a `Try`, so we can easily follow a success- or failure-path.

But what if some of the failures have _meaning_ to us, and in certain circumstances we want an otherwise failing scenario to succeed anyway?

Well, we can **recover** from failures and bend the code to our will. We can use the following method of `Try`, with a beautiful method signature, called `recover(Class<X> exception, Function<? super X,? extends T> f)`.

Its Javadoc reads:

> Returns this, if this is a Success or this is a Failure and the cause is not assignable from cause.getClass(). Otherwise tries to recover the exception of the failure with f, i.e. calling Try.of(() -> f.apply((X) getCause()).

In other words: for a specific type of exception we can provide a function which will turn our failure into success again.

First, get rid of the superfluous logging and the `onSuccess/onFailure` again. Right now we have a `Try`, a `map` for the success-scenario and a `getOrElseGet` for the error-scenario:

```groovy
class FeedHandler {
  
  List<Doc> handle(List<Doc> changes,
    Function<Doc, Try<Resource>> creator) {

    changes
      .findAll { doc -> isImportant(doc) }
      .collect { doc ->
        creator.apply(doc)
        .map { resource ->
          setToProcessed(doc, resource)
        }.getOrElseGet { e ->
          setToFailed(doc, e)
        }
      }
  }

  // ...

}
```

What if the the the "resource creation" API (i.e. `creator#apply` call) throws e.g. a `DuplicateResourceException` signalling the resource we're creating is a _duplicate_, it _already exists_.

We can use the `recover` function!

```groovy
  List<Doc> handle(List<Doc> changes,
    Function<Doc, Try<Resource>> creator) {

    changes
      .findAll { doc -> isImportant(doc) }
      .collect { doc ->
        creator.apply(doc)
        .recover { t ->
          handleDuplicate(doc)
        }.map { resource ->
          setToProcessed(doc, resource)
        }.getOrElseGet { e ->
          setToFailed(doc, e)
        }
      }
  }

  private Resource handleDuplicate(Doc alreadyProcessed) {
    // find earlier saved, existing resource and return that one
    return repository.findById(alreadyProcessed.getApiId())
  }

```

We could lookup a duplicate on our side (since it's already processed once), our "handleDuplicate" method returns _whatever the happy flow expects_ (i.e. a `Resource`) and processing continues as if nothing happened. 

Of course, this is just an example, but `recover` accepts any function which accepts a `Throwable` and returns a `Try` again.

## Many kinds of failure: pattern-matching 

* What if we actually need to be sure that we're only handling our "duplicate" situation _only_ in case of a `DuplicateResourceException` -- and not just _any_ exception, like now?

* What if the API can throw another type of exception we also need to handle specifically? How can we choose between handling multiple "choices" of exception types?

This is where the pattern-matching comes in, using the Match API of Vavr. We can create a `Match` object for the exception `x` (given to use by `recover`) while giving the static `of`-method several _cases_ to choose from.

```groovy
recover { x -> Match(x).of(
  Case($(instanceOf(DuplicateResourceException.class)), t -> handleDuplicate(doc)),
  Case($(instanceOf(SpecialException.class)),  t -> handleSpecial(t))
)}
```

This `$` is actually a static method of Vavr of which there are several overloaded versions which return a _pattern_. 

This version here is a so-called "guard-pattern" which accepts a `Predicate`. Check out another example from the Vavr Javadocs (in plain Java):

```java
String evenOrOdd(int num) {
  return Match(num).of(
    Case($(i -> i % 2 == 0), "even"),
    Case($(this::isOdd), "odd")
  );
}

boolean isOdd(int i) {
   return i % 2 == 1;
}
```

The combination of functions (`Case`, `$` and `Match`) seem a bit strange in Java, but there's no native support just yet. You could use [Vavr](https://www.vavr.io/) for this kind of functionality in the mean time.

_In Java 12 there already two preview features working hard to make all this a reality. It's [JEP 305: Pattern Matching for instanceof](http://openjdk.java.net/jeps/305) and [JEP 325: Switch Expressions](http://openjdk.java.net/jeps/325)_ 

In this installment we have seen that we can use failures as data, e.g. take an alternative path and return back to the functional flow, so to speak.

As reference, the code now looks:

```groovy
class FeedHandler {
  
  List<Doc> handle(List<Doc> changes,
    Function<Doc, Try<Resource>> creator) {

    changes
      .findAll { doc -> isImportant(doc) }
      .collect { doc ->
        creator.apply(doc)
        .recover { x -> Match(x).of(
          Case($(instanceOf(DuplicateResourceException.class)), t -> handleDuplicate(doc)),
          Case($(instanceOf(SpecialException.class)),  t -> handleSpecial(t))
        )}
        .map { resource ->
          setToProcessed(doc, resource)
        }.getOrElseGet { e ->
          setToFailed(doc, e)
        }
      }
  }

  private Resource handleDuplicate(Doc alreadyProcessed) {
    // find earlier saved, existing resource and return that one
    return repository.findById(alreadyProcessed.getApiId())
  }

  private Resource handleSpecial(SpecialException e) {
    // handle special situation
    return new Resource()
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

_Above example on GitHub failed to parse actually properly as Groovy, since the Groovy 2.x parser didn't understand lambda-syntax correctly, but of course you can also find the equivalent [working Java version](https://github.com/tvinke/fp-java-by-example/blob/master/src/main/groovy/example7/java/FeedHandlerJava.java)._

Go ahead, `Try` it yourself.