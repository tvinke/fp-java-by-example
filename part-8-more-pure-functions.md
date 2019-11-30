This is part 8, the last instalment of the series called "Functional Java by Example". 

The example I'm evolving in each part of the series is some kind of "feed handler" which processes documents. In the last instalment we've seen some pattern matching, using the Vavr library, and [treated failures as data too](https://tedvinke.wordpress.com/2019/04/29/functional-java-by-example-part-7-treat-failures-as-data-too/), e.g. take an alternative path and return back to the functional flow. 

In this last post of the series I'm taking __functions__ to the _extreme_: everything becomes a function.

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

## Maximizing the moving parts

You might have heard the following phrase by [Micheal Feathers](https://twitter.com/mfeathers):

> OO makes code understandable by encapsulating moving parts. FP makes code understandable by minimizing moving parts.

https://twitter.com/mfeathers/status/29581296216

Ok, let's forget about the failure-recovery in previous instalment for a bit and continue with a version like below:


```groovy
class FeedHandler {
  
  List<Doc> handle(List<Doc> changes,
    Function<Doc, Try<Resource>> creator) {

    changes
      .findAll { doc -> isImportant(doc) }
      .collect { doc ->
        creator.apply(doc)
        }.map { resource ->
          setToProcessed(doc, resource)
        }.getOrElseGet { e ->
          setToFailed(doc, e)
        }
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

### Replace by functional types

We can replace every method with a reference to a variable of a **functional interface** type, such as `Predicate` or `BiFunction`.

A) We can replace a method which accepts 1 argument which returns a __boolean__.
```groovy
private static boolean isImportant(doc) {
  doc.type == 'important'
}
```
by a **Predicate**
```groovy
private static Predicate<Doc> isImportant = { doc ->
  doc.type == 'important'
}
```

B) and we can replace a method that accepts 2 arguments and returns a result
```groovy
private static Doc setToProcessed(doc, resource) {
  ...
}

private static Doc setToFailed(doc, e) {
  ...
}
```
with a **BiFunction**
```groovy
private static BiFunction<Doc, Resource, Doc> setToProcessed = { doc, resource ->
  ...
}

private static BiFunction<Doc, Throwable, Doc> setToFailed = { doc, e ->
  ...
}
```

To actually invoke the logic encapsulated in a (Bi)Function we have to call `apply` on it. The result is the following:

```groovy
class FeedHandler {
  
  List<Doc> handle(List<Doc> changes,
    Function<Doc, Try<Resource>> creator) {

    changes
      .findAll { isImportant }
      .collect { doc ->
        creator.apply(doc)
        .map { resource ->
          setToProcessed.apply(doc, resource)
        }.getOrElseGet { e ->
          setToFailed.apply(doc, e)
        }
      }
  }
  
  private static Predicate<Doc> isImportant = { doc ->
    doc.type == 'important'
  }
  
  private static BiFunction<Doc, Resource, Doc> setToProcessed = { doc, resource ->
    doc.copyWith(
      status: 'processed',
      apiId: resource.id
    )
  }
  
  private static BiFunction<Doc, Throwable, Doc> setToFailed = { doc, e ->
    doc.copyWith(
      status: 'failed',
      error: e.message
    )
  }

}
```
### Moving all input to function itself

We're moving everything to the method signature so the caller of the FeedHandler's `handle` method can supply its own implementation of those functions.

The method signature will change from:
```groovy
List<Doc> handle(List<Doc> changes,
  Function<Doc, Try<Resource>> creator)
```
to
```groovy
List<Doc> handle(List<Doc> changes,
  Function<Doc, Try<Resource>> creator,
  Predicate<Doc> filter,
  BiFunction<Doc, Resource, Doc> successMapper,
  BiFunction<Doc, Throwable, Doc> failureMapper)
```

Second, we're renaming our original (static) **Predicate** and **BiFunction** variables 

* `isImportant`
* `setToProcessed`
* `setToFailed`

to new **constants** at the top of the class, reflecting their new role, resp. 

* `DEFAULT_FILTER`
* `DEFAULT_SUCCESS_MAPPER`
* `DEFAULT_FAILURE_MAPPER`

A client can fully control whether the default implementation is used for certain functions, or when custom logic needs to take over.

E.g. when only the failure-handling needs to be customized the `handle` method could be called like this:
```groovy
BiFunction<Doc, Throwable, Doc> customFailureMapper = { doc, e ->
  doc.copyWith(
    status: 'my-custom-fail-status',
    error: e.message
  )
}

new FeedHandler().handle(...,
  FeedHandler.DEFAULT_FILTER,
  FeedHandler.DEFAULT_SUCCESS_MAPPER,
  customFailureMapper
  )
```

_If your language supports it, you can make sure your client does not actually have to supply every parameter by assigning default values. I'm using [Apache Groovy](http://groovy-lang.org/) which supports assigning default values to parameters in a method:_

```groovy
List<Doc> handle(List<Doc> changes,
  Function<Doc, Try<Resource>> creator,
  Predicate<Doc> filter = DEFAULT_FILTER,
  BiFunction<Doc, Resource, Doc> successMapper = DEFAULT_SUCCESS_MAPPER,
  BiFunction<Doc, Throwable, Doc> failureMapper = DEFAULT_FAILURE_MAPPER)
```

Take a look at the code before we're going to apply one more change:

```groovy
class FeedHandler {

  private static final Predicate<Doc> DEFAULT_FILTER = { doc ->
    doc.type == 'important'
  }

  private static final BiFunction<Doc, Resource, Doc> DEFAULT_SUCCESS_MAPPER = { doc, resource ->
    doc.copyWith(
      status: 'processed',
      apiId: resource.id
    )
  }

  private static final BiFunction<Doc, Throwable, Doc> DEFAULT_FAILURE_MAPPER = { doc, e ->
    doc.copyWith(
      status: 'failed',
      error: e.message
    )
  }
  
  List<Doc> handle(List<Doc> changes,
                   Function<Doc, Try<Resource>> creator,
                   Predicate<Doc> filter = DEFAULT_FILTER,
                   BiFunction<Doc, Resource, Doc> successMapper = DEFAULT_SUCCESS_MAPPER,
                   BiFunction<Doc, Throwable, Doc> failureMapper = DEFAULT_FAILURE_MAPPER) {

    changes
      .findAll { filter }
      .collect { doc ->
        creator.apply(doc)
        .map { resource ->
          successMapper.apply(doc, resource)
        }.getOrElseGet { e ->
          failureMapper.apply(doc, e)
        }
      }
  }

}
```

### Introduce the Either

Have you noticed the following part?

```groovy
.collect { doc ->
  creator.apply(doc)
  .map { resource ->
    successMapper.apply(doc, resource)
  }.getOrElseGet { e ->
    failureMapper.apply(doc, e)
  }
}
```

Remember that the type of `creator` is `Function<Doc, Try<Resource>>`, meaning it returns a `Try`. We introduced **Try** in [part 7](https://tedvinke.wordpress.com/2019/04/29/functional-java-by-example-part-7-treat-failures-as-data-too/), borrowing it from languages such as Scala.

Luckily, the "doc" variable from `collect { doc ->` is still _in scope_ to pass to our `successMapper` and `failureMapper` which _need_ it, but there's a discrepancy between the method signature of `Try#map`, which accepts a **Function**, and our `successMapper`, which is a **BiFunction**. The same goes for `Try#getOrElseGet` -- it also needs just a **Function**.

From the Try Javadocs:
* `map(Function<? super T,? extends U> mapper)`
* `getOrElseGet(Function<? super Throwable,? extends T> other)`

Simply said, we need to go from

1. `BiFunction<Doc, Resource, Doc> successMapper`
2. `BiFunction<Doc, Throwable, Doc> failureMapper`

to

1. `Function<Resource, Doc> successMapper`
2. `Function<Throwable, Doc> failureMapper`

while still being able to have the original document as _input_ too.

Let's introduce two simple types encapsulating the 2 arguments of the 2 BiFunctions:

```groovy
class CreationSuccess {
  Doc doc
  Resource resource
}

class CreationFailed {
  Doc doc
  Exception e
}
```

We change the arguments from

1. `BiFunction<Doc, Resource, Doc> successMapper`
2. `BiFunction<Doc, Throwable, Doc> failureMapper`

to a **Function** instead:

1. `Function<CreationSuccess, Doc> successMapper`
2. `Function<CreationFailed, Doc> failureMapper`

The `handle` method now looks like:

```groovy
List<Doc> handle(List<Doc> changes,
                 Function<Doc, Try<Resource>> creator,
                 Predicate<Doc> filter,
                 Function<CreationSuccess, Doc> successMapper,
                 Function<CreationFailed, Doc> failureMapper) {

  changes
    .findAll { filter }
    .collect { doc ->
      creator.apply(doc)
      .map(successMapper)
      .getOrElseGet(failureMapper)
    }
}
```

...__but it does not work yet__.

The `Try<Resource>` makes `map` and `getOrElseGet` require resp. a
* `Function<Resource, Doc> successMapper`
* `Function<Throwable, Doc> failureMapper`

That's why we need to change it to another famous FP construct, called an **Either**.

Luckily Vavr has an [Either](https://static.javadoc.io/io.vavr/vavr/0.9.2/io/vavr/control/Either.html) too. Its Javadoc says:

> Either represents a value of two possible types.

The Either type is usually used to discriminate between a value which is either correct ("right") or an error.

It gets abstract pretty fast:

> An Either is either a Either.Left or a Either.Right. If the given Either is a Right and projected to a Left, the Left operations have no effect on the Right value. If the given Either is a Left and projected to a Right, the Right operations have no effect on the Left value. If a Left is projected to a Left or a Right is projected to a Right, the operations have an effect.

Let me explain above cryptic documentation. If we replace
```groovy
Function<Doc, Try<Resource>> creator
```
by
```groovy
Function<Doc, Either<CreationFailed, CreationSuccess>> creator
```

we assign `CreationFailed` to the "left" argument which by convention usually holds the error (see [Haskell docs on Either](http://hackage.haskell.org/package/base-4.12.0.0/docs/Data-Either.html)) and `CreationSuccess` is the "right" (and "correct") value.

At run-time the implementation used to return a `Try`, but now it can return an **Either.Right** in case of success e.g.
```groovy
return Either.right(
  new CreationSuccess(
    doc: document,
    resource: [id: '7']
  )
)
```
or **Either.Left** with the exception in case of failure -- and __both including the original document too__. Yes.

Because now ultimately the types match, we finally squash

```groovy
.collect { doc ->
  creator.apply(doc)
  .map { resource ->
    successMapper.apply(doc, resource)
  }.getOrElseGet { e ->
    failureMapper.apply(doc, e)
  }
}
```

into

```groovy
.collect { doc ->
  creator.apply(doc)
  .map(successMapper)
  .getOrElseGet(failureMapper)
}
```

The `handle` method now looks like:

```groovy
List<Doc> handle(List<Doc> changes,
                 Function<Doc, Either<CreationFailed, CreationSuccess>> creator,
                 Predicate<Doc> filter,
                 Function<CreationSuccess, Doc> successMapper,
                 Function<CreationFailed, Doc> failureMapper) {

  changes
    .findAll { filter }
    .collect { doc ->
      creator.apply(doc)
      .map(successMapper)
      .getOrElseGet(failureMapper)
    }
}
```
## Conclusion

I can say that I've met most of the goals I laid out in the beginning:

* Yes, I managed to avoid re-assigning variables
* Yes, I managed to avoid mutable data structures
* Yes, I managed to avoid state (well, at least in the FeedHandler)
* Yes, I managed to favor functions (using some of Java's built-in functional types and some of 3rd-party library Vavr)

We've moved everything to the function signature so the caller of the FeedHandler's `handle` method can pass directly the correct implementations. If you look back all the way to the [initial version](https://tedvinke.wordpress.com/2017/11/07/functional-java-by-example-part-1-from-imperative-to-declarative/) you'll notice that we still have all the responsibilities while processing a list of changes:

* filtering a list of documents by some criteria
* creating a resource per document
* do something when the resource has been created successfully
* do something else when the resource could not be created

However, in the first part these responsibilities were written out _imperatively_, statement for statement, all clumped together in one big `handle` method. Now, at the end, every decision or action is represented by a function with abstract names, such as "filter", "creator", "successMapper" and "failureMapper". Effectively, it became a higher-order function, taking one of more functions as an argument. The responsibility of providing all the arguments has been shifted a level up the stack, to the client. If you look at the [GitHub project](https://github.com/tvinke/fp-java-by-example) you'll notice that for these examples I had to update the unit tests constantly.

### The debatable parts

In practice I would probably not write my (Java) business code like how the `FeedHandler` class has become with regard to the use of passing in generic Java functional types (i.e. `Function`, `BiFunction`, `Predicate`, `Consumer`, `Supplier`), if I do not need all this extreme flexibility. All of this comes at the cost of readability. Yes, Java is a statically typed language so, using generics, one has to be **explicit in all of the type parameters**, leading to a difficult function signature of:

> handle(List<Doc> changes, Function<Doc, Either<CreationFailed, CreationSuccess>> creator, Predicate<Doc> filter, Function<CreationSuccess, Doc> successMapper, Function<CreationFailed, Doc> failureMapper)

In plain JavaScript you'd have none of the types, and you'd have to read the documentation to know what's expected of each argument.

> handle = function(changes, creator, filter, successMapper, failureMapper)

But hey, its a trade-off. Groovy, also a JVM language, _would_ allow me to omit the type information in all the examples in this series, and even allowed me to use Closures (like lambda expressions in Java) are at the core of the functional programming paradigm in Groovy.

More extreme would be specifying all the types at the _class-level_ for maximum flexibility for the client to specify different types for different `FeedHandler` instances.

> handle(List<T> changes, Function<T, Either<R, S>> creator, Predicate<T> filter, Function<S, T> successMapper, Function<R, T> failureMapper)

When is this appropriate?
* If you have full control of your code, when it is used in a specific context to solve a specific problem, this would be way too much _abstractness_ to yield any benefits.
* However, If I would open-source a library or framework to the world (or maybe within an organisation to other teams or departments) which is being used in various different use cases I can't all think of beforehand, _designing_ for flexibility is probably worth it. Let callers decide how to filter and what constitutes success or failure can be a smart move.

Ultimately above touches a bit on **API design**, yes, and **decoupling**, but "making everything a function" in a typical Enterprise(tm) Java project probably warrants some discussion with you and your teammates. Some colleagues are accustomed over the years to a more traditional, idiomatic way of writing code.

### The good parts

* I'd definitely [prefer immutable data structures](https://tedvinke.wordpress.com/2018/06/15/functional-java-by-example-part-4-prefer-immutability/) (and "referential transparency") to help in reasoning about the state my data is in. Think of `Collections.unmodifiableCollection` for collections. In my examples I used Groovy's `@Immutable` for POJOs, but in plain Java libraries such as [Immutables](https://immutables.github.io/), [AutoValue](https://github.com/google/auto) or [Project Lombok](https://projectlombok.org/) can be used.
* The biggest improvement was actually the _leading up_ to a more functional style: making the code [tell a story](https://tedvinke.wordpress.com/2017/11/24/functional-java-by-example-part-2-tell-a-story/), which was mainly about separating concerns and naming things appropriately. This is a good practice in any style of programming (even OO :D), but this really cleared up the clutter and allowed to introduce (pure) functions at all.
* In Java we're so accustomed to doing exception handling a specific way, that it's hard for developers like me to come up with _other_ solutions. A functional language like Haskell just returns error codes, because ["Niklaus Wirth considered exceptions to be the reincarnation of GOTO and thus omitted them"](https://wiki.haskell.org/Exception). In Java one can use a `CompletableFuture` or ...
* **specific types such as `Try` and `Either`**, usable in your own codebase by introducing a 3rd-party library such as Vavr, can help a great deal in enabling _more options_ writing in a FP style! I was very charmed by the elegance of writing 'success' or 'failure' paths in a fluent way and being very readable. 

Java isn't Scala or Haskell or Clojure of F# and it originally followed an object-oriented programming (OOP) paradigm, just like C++, C#, Ruby, etc, but after the introduction of lambda expressions in Java 8 and combined with some awesome open-source libraries out there developers are nowadays definitely able to **pick 'n mix the best elements what OOP and FP have to offer**. 

## Lessons learned of doing a series

I started this series _waay too long_ ago. Back in 2017 I found myself doing several FP-style-inspired refactorings on a piece of code, which inspired me to find an example for a series of articles, dubbed _"Functional Java by Example"_. This became the `FeedHandler` code I've been using throughout each instalment. 

I already did all the individual code changes back then, but at the time I planned to write the actual blogposts I often thought: "I just can't show just the refactoring, I have to actually explain things!" That's where I sort of laid the trap for myself as throughout time I got less and less time to actually sit down and _write_. (Anyone who ever wrote a blog knows the difference in time effort of simply sharing a gist and writing coherent paragraphs of understandable English ;-) )

Next time when I think of doing a series, I'll Google back for some of these lessons learned:

1. Don't include a table of contents (TOC) at the top of each article, if you're not prepared to update all the links every time of each previously published instalment when you publish a new article. And if you cross-post these to the company's corporate blog it's 2 times as much work :-)
2. Over time you might come to the conclusion you'd rather deviate from your primary use case, your Big Coding Example you started out with. I'd rather showcased many more FP-concepts -- such as currying, memoization, laziness, and also a _different mindset when using FP techniques_ -- but I could not really well fit that in within previously done refactorings and the TOC I established at the beginning. If you're writing about a specific concept, one usually finds an appropriate example helping explain the particular concept at hand, and still relating to the reader. With time, I experienced, comes better insight in determining what better to write about next and what more-appropriate examples to use. Next time I'll have to find a way to give (better: allow) myself some creative freedom along the way ;-)

## Read more

* **[Functional Thinking: Paradigm Over Syntax](https://www.amazon.com/Functional-Thinking-Paradigm-Over-Syntax/dp/1449365515)** Amazing book by Neil Ford, which shows a new way of FP thinking and also approaching problems differently.
* **[Functional Programming in 40 Minutes](https://www.youtube.com/watch?v=0if71HOyVjY&t=295s)** Youtube video of Russ Olsen explaining "it takes 379 pages for these mathematicians to prove 1+1=2. Let's see what good ideas we can steal from them" :)
* **[Why Isn't Functional Programming the Norm?](https://www.youtube.com/watch?v=QyJZzq0v7Z4)** Youtube video of Richard Feldman where he explains why OOP became very popular and why FP isn't the norm. He's a member of the Elm core team and, as you can tell, has some affinity with FP.
* **[Inversion of (Coupling) Control](https://sagenschneider.blogspot.com/2019/02/inversion-of-coupling-control.html)** Food-for-thought article about "managed functions". You wanted abstract?