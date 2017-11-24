**Functional Programming (FP) is about avoiding reassigning variables, avoiding mutable data structures, avoiding state and favoring functions all-the-way. What can we learn from FP if we would apply functional techniques to our everyday Java code?** 

In this series called "Functional Java by Example" I will refactor in 8 installments an existing piece of code to see if I can reach _Functional Nirvana_ in Java. 

I don't have much experience in a "real" functional language such as Haskell or F#, but I hope to demonstrate in each article by example what it means to apply some of these practices to your every day Java code. 

Hopefully at the end you've gained some insight and know to pick some techniques which would benefit your own codebase.

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

_Disclaimer: code is written in [Apache Groovy](http://groovy-lang.org/), primarily for conciseness, so I don't have to type stuff (you know: typing) where it __doesn't__ matter for the examples. Secondary, this language Just Makes Me Happy._

### Why should you care about Functional Programming (FP)?

If you're not doing Haskell, F# or Scala on a hip real-time, streaming data event processing framework you might as well pack your bags. Even the JavaScript guys are spinning functions around your methods these days -- and that language has been around for some time already. 

There are a lot of articles and video's out there which make you believe that if you _don't_ hop on the Functional bandwagon these days, you're left behind with your old OOP-contraptions and frankly, are obsolete within a couple of years.

Well, I'm here to tell you that's _not_ entirely true, but FP _does_ have some premises, such as **readability, testability and maintainability**, values which we also strive to achieve in our (enterprise) Java code right?

As you're reading this, for years you might already have the same outspoken opinion about FP being a step [forwards](http://prettyprint.me/prettyprint.me/2009/08/01/why-functional-languages-rock-with-multi-core/index.html) or [backwards](https://www.infoworld.com/article/2615766/application-development/functional-programming--a-step-backward.html) or anno 2017-2018 you are just **open for new ideas** :-) 

> You can level up your skills in every language by learning FP. 

Determine for yourself what _you_ can learn from it and how your own programming can benefit from it.

If you're up to the task, let's start this series with...


## Some existing code

_**A word about example code:** It's pretty tricky to come up with contrived examples for blogs like these: it should be easy enough to appeal to a broad audience, simple enough to be understood without too much context, but still be interesting enough to result in desired learning effects._

Moving forward, each installment in this series will build on the previous one. Below is the code we're going to take as a starting point.

So, put on your glasses and see if you're familiar with coding-style below.


```groovy
class FeedHandler {

  Webservice webservice
  DocumentDb documentDb

  void handle(List<Doc> changes) {

    for (int i = 0; i < changes.size(); i++) {
      def doc = changes[i]
      if (doc.type == 'important') {

        try {
          def resource = webservice.create(doc)
          doc.apiId = resource.id
          doc.status = 'processed'
        } catch (e) {
          doc.status = 'failed'
          doc.error = e.message
        }
        documentDb.update(doc)
      }
    }
  }
}
```

* It's some sort of `FeedHandler`. 
* It has two properties, some `Webservice` class and a `DocumentDb` class. 
* There's a `handle` method which does something with a list of `Doc` objects. Documents?

**Try to figure out what's going on here :-)**

..

..

..

Done?

Reading stuff like this can make you feel like a human parser sometimes. 

Scanning the class name (`FeedHandler?`) and the one method (`void handle`) can give you, next to some eye sore, a "feel" for the purpose of everything.

However, figuring out what exactly gets "handled" _inside the `handle` method_ is much harder.

* There's a _`for-loop`_ there -- but what's exactly being iterated? How many times?
* This variable `webservice` is called, returning something called `resource`. 
* If `webservice` returns successfully, the `doc` (a document?) being iterated over is updated with a status. 
* Seems `webservice` can also throw an `Exception`, which is caught and the document is updated with another status.
* Ultimately, the document is "updated" by this `documentDb` instance. Looks like a database.
* _Oh wait, this happens only for the "important" docs_ -- a `doc.type` is checked first before doing all above stuff.

Perhaps, you have heard of the phrase:

> Code is read more than it is written.

Check out this piece of beauty:

```groovy
for (int i = 0; i < changes.size(); i++) {
```

Above code is written in an _imperative_ style, which means that the concrete statements -- which manipulate state and behaviour -- are written out explicitly.

* Initialize an `int i` with zero
* Loop while `int i` is less then the size of the `changes` list
* Increment `int i` with 1 each iteration

In this style of _imperative_ (procedural) coding (which most of the mainstream languages, including object-oriented programming (OOP) languages, such as Java, C++, C#, were designed to primarily support) a developer writes the exact statements a computer needs to perform to accomplish a certain task.

A few signals of very _imperative_ (procedural) code:

1. Focus on **how** to perform the task
1. State changes and order of execution is important
1. Many loops and conditionals

The code clearly focuses on the "How" -- which makes the "What" hard to determine.

## Focus on the What

Our first step, as the title of this article already have away, is to move away from the _imperative_ style of coding and refactor to a more _declarative_ style -- of which FP is a form.

The _loop_ is bugging me the most.

Here's the new version of the code.

```groovy
class FeedHandler {

  Webservice webservice
  DocumentDb documentDb

  void handle(List<Doc> changes) {

    // for (int i = 0; i < changes.size(); i++) {
    //    def doc = changes[i]
    changes
      .findAll { doc -> doc.type == 'important' }
      .each { doc ->

      try {
        def resource = webservice.create(doc)
        doc.apiId = resource.id
        doc.status = 'processed'
      } catch (e) {
        doc.status = 'failed'
        doc.error = e.message
      }
      documentDb.update(doc)
    }
  }
}
```

What's changed?

* The `if (doc.type == 'important')` part has been replaced with a `findAll { doc -> doc.type == 'important' }` again on the document collection itself -- _meaning "find all documents which are important and return a new collection with only those important documents"_
* The imperative `for-loop` (with the intermediate `i` variable) has been replaced by the declarative `each` method on the documents collection itself -- _meaning "execute the piece of code for each doc in the list and I don't care how you do it"_ :-)

_Don't worry about `each` and `findAll`: these methods are added by Groovy, which I use happily together with Java in the same code base, to any Collection, e.g. Set, List, Map. Vanilla Java 8 has equivalent mechanisms, such as `forEach` to iterate a collection more declaratively._

What leads to readable software is:

> Describe _"What"_ and not _"How"_.

I can easily see what's going on if I write my code in a more _functional_ style, which **saves me time** (because yes, I do _read_ code 90% of the time instead of writing it) and writing it like this is **less error-prone**, because less lines gives less opportunity for bugs to hide.

This is it for now :-) 

In part 2, we will _tell a story_ properly, paving the way for more functional programming, such as "Either" or "Try" even later in the series.

--

_Follow me on [@tvinke](https://twitter.com/tvinke) if you like what you're reading or subscribe to my blog on [https://tedvinke.wordpress.com](https://tedvinke.wordpress.com)._