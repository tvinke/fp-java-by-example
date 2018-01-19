This is part 3 of the series called "Functional Java by Example". 

The example I'm evolving in each part of the series is some kind of "feed handler" which processes documents. In previous parts I started with some original code and applied some refactorings to describe "what" instead of "how". 

In order to help the code going forward, we need to **get rid of the good ol' `java.lang.Exception`.  (Disclaimer: we can't actually get rid of it)** That's where this part comes in.

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


## Getting up to speed about Exceptions

Our `java.lang.Exception` has been around since Java 1.0 - and has basically been our friend in good times and nemesis at other times. 

There's not much to talk about them, but if you want to read up on a few sources, here are my favorites:

* [Exceptions in Java](https://www.javaworld.com/article/2076700/core-java/exceptions-in-java.html) (JavaWorld)
* [Exceptions in Java - GeeksforGeeks](https://www.geeksforgeeks.org/exceptions-in-java/) (geeksforgeeks.org)
* [9 Best Practices to Handle Exceptions in Java](https://stackify.com/best-practices-exceptions-java/) (stackify.com)
* [Best Practices for Exception Handling](http://www.onjava.com/pub/a/onjava/2003/11/19/exceptions.html) (onjava.com)
* [Java Exception Interview Questions and Answers](https://www.journaldev.com/2167/java-exception-interview-questions-and-answers) (journaldev.com)
* [Exception handling in java with examples](https://beginnersbook.com/2013/04/java-exception-handling/) (beginnersbook.com)
* [Java Exception Handling (Try-catch)](https://www.hackerrank.com/challenges/java-exception-handling-try-catch/forum) (hackerrank.com)
* [Top 20 Java Exception Handling Best Practices - HowToDoInJava](https://howtodoinjava.com/best-practices/java-exception-handling-best-practices/) (howtodoinjava.com)
* [Exception Handling & Assertion in Java - NTU](https://www.ntu.edu.sg/home/ehchua/programming/java/J5a_ExceptionAssert.html) (ntu.edu.sg)
* [Exception Handling: A Best Practice Guide](https://dzone.com/articles/exception-handling-a-best-practice-guide) (dzone.com)
* [9 Best Practices to Handle Exceptions in Java](https://dzone.com/articles/9-best-practices-to-handle-exceptions-in-java) (dzone.com)
* [Fixing 7 Common Java Exception Handling Mistakes](https://dzone.com/articles/fixing-7-common-java-exception-handling-mistakes) (dzone.com)
* [Java Practices -> Checked versus unchecked exceptions](http://www.javapractices.com/topic/TopicAction.do?Id=129) (javapractices.com)
* [Common mistakes with exceptions in Java | Mikael Ståldal's technical blog](https://www.staldal.nu/tech/2007/05/21/common-mistakes-with-exceptions-in-java/) (staldal.nu)
* [11 Mistakes Java Developers Make When Using Exceptions](https://medium.com/@rafacdelnero/11-mistakes-java-developers-make-when-using-exceptions-af481a153397) (medium.com/@rafacdelnero)
* [Are checked exceptions good or bad?](https://www.javaworld.com/article/3142626/core-java/are-checked-exceptions-good-or-bad.html) (JavaWorld)
* [Checked exceptions: Java's biggest mistake | Literate Java](http://literatejava.com/exceptions/checked-exceptions-javas-biggest-mistake/) (literatejava.com)
* [Unchecked Exceptions—The Controversy](https://docs.oracle.com/javase/tutorial/essential/exceptions/runtime.html) (docs.oracle.com)
* [The Trouble with Checked Exceptions](http://www.artima.com/intv/handcuffs2.html) (artima.com)
* [Exceptions in Java: You're (Probably) Doing It Wrong](https://dzone.com/articles/how-to-properly-use-exceptions-in-java) (dzone.com)
* [Java theory and practice: The exceptions debate - IBM](https://www.ibm.com/developerworks/library/j-jtp05254/index.html) (ibm.com)
* [Java's checked exceptions were a mistake (and here's what I would like to do about it](http://radio-weblogs.com/0122027/stories/2003/04/01/JavasCheckedExceptionsWereAMistake.html) (radio-weblogs.com)
* [Buggy Java Code: Top 10 Most Common Mistakes That Java Developers Make | Toptal](https://www.toptal.com/java/top-10-most-common-java-development-mistakes) (toptal.com)


You on Java 8 already? Life became so much better! I... _Err...oh, wait._

* [Error handling with Java input streams - Javamex](https://www.javamex.com/tutorials/io/input_stream_error_handling.shtml) (javamex.com)
* [Handling checked exceptions in Java streams](https://www.oreilly.com/ideas/handling-checked-exceptions-in-java-streams) (oreilly.com)
* [Exceptional Exception Handling In JDK 8 Streams](https://www.azul.com/exceptional-exception-handling-jdk-8-streams/) (azul.com)
* [Java 8 Functional Interfaces with Exceptions](http://slieb.org/blog/throwable-interfaces/) (slieb.org)
* [Repackaging Exceptions In Streams - blog@CodeFX](https://blog.codefx.org/java/repackaging-exceptions-streams/) (blog.codefx.org)
* [How to handle Exception in Java 8 Stream? - Stack Overflow](https://stackoverflow.com/questions/43383475/how-to-handle-exception-in-java-8-stream) (stackoverflow.com)
* [Checked Exceptions and Streams | Benji's Blog](http://benjiweber.co.uk/blog/2014/03/22/checked-exceptions-and-streams/) (benjiweber.co.uk)
* [A story of Checked Exceptions and Java 8 Lambda Expressions](https://javadevguy.wordpress.com/2016/02/22/a-story-of-checked-exceptions-and-java-8-lambda-expressions/) (javadevguy.wordpress.com) - _nice war story!_
* [hgwood/java8-streams-and-exceptions](https://github.com/hgwood/java8-streams-and-exceptions) (github.com)
* ...

Ok, seems that there's no way you can actually do it _right_.

At least, after reading above list, we're now completely _up-to-speed_ on the topic :-)

Luckily I don't have to write a blog post any more about what's been covered for 95% already in above articles, but I'll focus here on the one `Exception` we actually have in the code :-) 

## Side effects

Since you're reading this post, you're probably interested in why this all has to do with _functional programming_. 

On the road to approaching your code in a more "functional way", you may have encountered the term "side effect" and that it's a "bad thing". 

In the real world, a **side effect is something you did not intend to happen**, and you might say it's equivalent to an "exceptional" situation (you would indicate with an exception), but it has a more strict meaning in a Functional Programming context.

The Wikipedia-article about a [Side effect](https://en.wikipedia.org/wiki/Side_effect_(computer_science)) says:

> Side effect (computer science) In computer science, a function or expression is said to have a side effect if it modifies some state outside its scope or has an observable interaction with its calling functions or the outside world besides returning a value. ... In functional programming, side effects are rarely used.


So let's see how our FeedHandler code currently looks like after the first two articles in this series:

```groovy
class FeedHandler {

  Webservice webservice
  DocumentDb documentDb

  void handle(List<Doc> changes) {

    changes
      .findAll { doc -> isImportant(doc) }
      .each { doc ->

      try {
        def resource = createResource(doc)
        updateToProcessed(doc, resource)
      } catch (e) {
        updateToFailed(doc, e)
      }
    }
  }

  private Resource createResource(doc) {
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

There's one place where we _try-catch_ exceptions, and that's where we _loop through the important documents_ and try to create a "resource" (whatever that is) for it. 

```groovy
try {
  def resource = createResource(doc)
  updateToProcessed(doc, resource)
} catch (e) {
  updateToFailed(doc, e)
}
```

_In code above `catch (e)` is Groovy shorthand for `catch (Exception e)`._

Yes, that's the generic `java.lang.Exception` which we're catching. Could be any exception, including NPE. 

If there's no exception thrown from the `createResource` method, we update the document ("doc") to 'processed', else we update it to 'failed'. _BTW, even `updateToProcessed` can throw an exception too, but for the current discussion I'm actually only interested in a successful resource creation._

So, above code **works** (I've got the unit tests to prove it :-)) but I'm not happy with the `try-catch` statement as it is now. I'm only interested in successful resource creation, and, silly me, I could only come up with `createResource` either returning a successful resource _or_ throwing an exception. 

Throwing an exception to signal something went wrong, get the hell out of dodge, have caller _catch_ the exception in order to handle it, is why exceptions were invented right? And it's better than returning `null` right?

It happens all the time. Take some of our favorite frameworks, such as `EntityManager#find` from the [JPA spec](https://docs.oracle.com/javaee/7/api/javax/persistence/EntityManager.html#find-java.lang.Class-java.lang.Object-):

<screenshot-image-entity-manager-find>

Arg! Returns `null`. 

> **Returns:**
  the found entity instance or null if the entity does not exist

Wrong example.

Functional Programming encourages side-effect free methods (or: functions), to make the code more understandable and easier to reason about. If a method just accepts certain input and returns the same output every time - which makes it a _pure_ function - all kinds of optimizations can happen under the hood e.g. by the compiler, or caching, parallelisation etc. 

We can replace _pure_ functions again by their (calculated) value, which is called [referential transparancy](https://en.wikipedia.org/wiki/Referential_transparency).

In previous article, we'll already extracted some logic into methods of their own, such as `isImportant` below. Given the _same_ document (with the _same_ `type` property) as input, we'll get the _same_ (boolean) output every time.

```groovy
boolean isImportant(doc) {
  doc.type == 'important'
}
```

Here there's no _observable_ side effect, no global variables are mutated, no log file is updated - it's just _stuff in, stuff out_.

Thus, I would say that functions which interact with the outside world through our traditional exceptions are _rarely_ used in functional programming.

I want to do _better_ than that. _Be_ better. :-)


## Optional to the rescue

As Benji Weber [expresses](http://benjiweber.co.uk/blog/2014/03/22/checked-exceptions-and-streams/) it:

> There are different viewpoints on how to use exceptions effectively in Java. Some people like checked exceptions, some argue they are a failed experiment and prefer exclusive use of unchecked exceptions. Others eschew exceptions entirely in favour of passing and returning types like Optional or Maybe.

Ok, let's try Java 8's `Optional` so signal whether a resource can or can not be created.

Let's change the our webservice interface and `createResource` method to wrap and return our resource in an `Optional`:
```groovy
//private Resource createResource(doc) {
private Optional<Resource> createResource(doc) {
  webservice.create(doc)
}

```

Let's change the original `try-catch`:
```groovy
try {
  def resource = createResource(doc)
  updateToProcessed(doc, resource)
} catch (e) {
  updateToFailed(doc, e)
}
```
to `map` (processing resource) and `orElseGet` (processing empty optional):
```groovy
createResource(doc)
  .map { resource ->
    updateToProcessed(doc, resource)
  }
  .orElseGet { /* e -> */
    updateToFailed(doc, e)
  }
```

Great `createResource` method: either correct result comes back, or an empty result.

Wait a minute! The exception `e` we need to pass into `updateToFailed` is _gone_: we have an empty `Optional` instead. We can't store the reason **why** it failed -- which we do need.

May be an `Optional` just signals "absence" and is a wrong tool for our purpose here.

## Exceptional completion

Without the `try-catch` and with the `map-orElseGet` instead, I _do_ like the way the code started to reflect the "flow" of operations more. Unfortunately, using `Optional` was more appropriate for "getting something" or "getting nothing" (which names like `map` and `orElseGet` also suggested) and didn't give us the opportunity to record a reason for failing.

What's another way to either get the successful result or get the reason for failing, still approaching our nice way of reading?

A `Future`. Better yet: a `CompletableFuture`.

A `CompletableFuture` (CF) knows how to return a value , in this way it's similar to an `Optional`. Usually a CF is used for getting a value _set in the future_, but that's not what we want to use it for...

From the [Javadoc](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletableFuture.html):

> A Future that ..., supporting ... actions that trigger upon its completion.

Jip, it can signal _"exceptional" completion_ -- giving me the opportunity to act upon it.

Let's change the `map` and `orElseGet`:
```groovy
createResource(doc)
  .map { resource ->
    updateToProcessed(doc, resource)
  }
  .orElseGet { /* e -> */
    updateToFailed(doc, e)
  }
```
to `thenAccept` (processing success) and `exceptionally` (processing failure):
```groovy
createResource(doc)
  .thenAccept { resource ->
    updateToProcessed(doc, resource)
  }
  .exceptionally { e ->
    updateToFailed(doc, e)
  }
```

The `CompletableFuture#exceptionally` method accepts a function with our exception `e` with the actual reason for failure.

You might think: _tomayto, tomahto. First we had `try-catch` and now we have `thenAccept-exceptionally`, so what's the big difference?_ 

Well, we can obviously not get rid of the exceptional situations, but we're now thinking like a resident of Functionalville would: our methods start to become _functions_, telling us something goes in and something goes out. 

Consider it a small refactoring we need towards part 4, limiting the amount of side effects in our code even more, and part 5.

This is it for now :-) 

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

--

_Follow me on [@tvinke](https://twitter.com/tvinke) if you like what you're reading or subscribe to my blog on [https://tedvinke.wordpress.com](https://tedvinke.wordpress.com)._