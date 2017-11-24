This is part 2 of the series called "Functional Java by Example". 

The example I'm evolving in each part of the series is some kind of "feed handler" which processes documents. In previous part I started with some original code and applied some refactorings to describe "what" instead of "how". 

In order to help the code going forward, we need to **tell a story** first. That's where this part comes in.

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

As a reference, we now have the following code as a starting point:

```groovy
class FeedHandler {

  Webservice webservice
  DocumentDb documentDb

  void handle(List<Doc> changes) {

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

## Read out aloud

When I first started using [Spock](http://spockframework.org/) as a testing framework, since it came by default with [Grails](https://grails.org/) many years ago, I was impressed (and still am) by its many features and ease of use.

You know what Mocks, Stubs and Spies are, right? Mockito has them, Powermock has them and basically every other serious (unit) testing framework. The concept of a Mock isn't hard to grasp (you can read all about it [here](https://www.google.com/search?q=mocks+and+stubs)), but Spock has _a special way of describing the (expected) interactions with its mocks._ 

There a great chapter about ["Interaction Based Testing"](http://spockframework.org/spock/docs/1.1/interaction_based_testing.html) which explains how to write down these interactions with a code sample. 

The "Mocking" sub-chapter starts with:

> Mocking is the act of describing (mandatory) interactions between the object under specification and its collaborators. Here is an example:

```groovy
def "should send messages to all subscribers"() {
  when:
  publisher.send("hello")

  then:
  1 * subscriber.receive("hello")
  1 * subscriber2.receive("hello")
}
```

If you're not familiar with Spock, with Groovy, or merely with above style of writing, don't worry! 

**The author(s) of above Spock documentation too recognized not everyone immediately would understand what's happening here.**

They help with some **advice** and continue the documentation:

> **Read out aloud**: "When the publisher sends a 'hello' message, then both subscribers should receive that message exactly once."


The emphasis on "Read out aloud" is mine, because I think it's pretty important. Not going into more Spock details here, but the advice itself I try to take to heart in my day-to-day coding.

* When I write a piece of code, can I read it out aloud?
* When someone else reads my code, can he/she read it out aloud?

"Aloud" here has _nothing to do with volume_ :-) but with the fact that you can describe "what's happening here" in a concise and understandable way. This makes it easy to reason about code.

## High-level vs low-level

Consider the following scenario: 

After driving for hours around in an unknown city to find the Theatre, after your car navigation has broken down, you finally decided to stop and ask for directions. 

You stop near a pedestrian:

You: 
> _Sir, would you happen to know how to get to the Theater from here?_

Pedestrian: 
> 1. _Sure, that's pretty easy. Here we go:_
> 2. _Check your windows to make sure you have good visibility. Check mirrors to be sure they are aligned properly, giving you a proper view of the road._
> 3. _Adjust the seat so that your feet comfortably reach both pedals._
> 4. _Close the window._
> 5. _Reset the turn signal._
> 6. _Disengage your parking brake before you start driving._
> 7. _Ah, I see that you have an automatic. Please put the gear stick in "Drive"._
> 8. _Slowly, take your foot of the brake and monitor your dashboard gauges._
> 9. _Straight on, increase speed, monitoring your speedometer, keeping the RPM around 2000_
> 10. _After around 120 yards, indicate with your blinkers for at least two seconds before beginning to switch to the left lane._
> 11. _Slowly move the car into the other lane. Turn your hands on the steering wheel ever so slightly in order to change lanes. It only takes a very slight movement of the wheel; as most modern cars are fitted with power steering. It should take anywhere from one to three seconds for you to change lanes. Any less and you're doing it too quickly; any more and you're doing it too slowly._
> 12. Another X steps...
> 38. Good luck!

Or, consider an alternative universe where the conversation would go like this:

You: 
> _Sir, would you happen to know how to get to the Theater from here?_

Pedestrian: 
> 1. _Sure, that's pretty easy. Here we go:_
> 2. _Take a left turn and cross the bridge. It's on your right._ 
> 3. _Good luck!_

The last scenario is a breeze: clear directions what to do & where to go!

The first scenario, however, is **ridden by details** -- with low-level specifics about driving the car itself -- and even while we would not hope to get directions like that in real-life we still write software like that. 

**Tell me something on the right level. If I need specifics I'll ask for it.** 

_(BTW [wikihow.com: How to Drive a Car](https://www.wikihow.com/Drive-a-Car) kindly donated some of above instructions. If you actually need to learn to drive, it has a ton o' resources!)_

Telling something on the right level, means not only using properly named classes and methods, but also using the **right kind of abstractions** in them.

Let's take a look again at our code:

```groovy
class FeedHandler {

  Webservice webservice
  DocumentDb documentDb

  void handle(List<Doc> changes) {

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

## The story

How can we combine "read out aloud" and "high-level vs low-level" in our code?

What does our single `handle` method currently read like?

> 1. _Find all documents where the `type`-property equals the string `"important"`._
> 2. _Call `create` on `webservice` with the document, which returns a resource._
> 3. _If we have a resource, assign the resource's `id` to the documents `apiId` property._
> 4. _Set the `status` property of the document to the string `"processed"`._
> 5. _If an exception occurred, set the `status` property of the document to the string `"failed"`. Set the `status` property of the document to the `message` from the exception._
> 6. _Finally, call `update` on `documentDb` with the document._

Basically this is just repeating the code statements!

What story I'd like to tell **instead**, is the following:

> 1. Process "important" documents by "creating a resource" through a webservice.
> 2. Every time when this succeeds, associate both together and "mark the document as processed", else mark it as "failed".

Reads pretty well, don't you think?

We can actually make this happen by using several "Extract method" refactorings in our IDE and choosing some good names for the extracted methods.

The double-quoted phrases in above story are the important bits I want to see at the high-level. 

### "important" 

Why do I care what attribute is used of a document to determine it's importance? Now it's the string `"important"` which indicates "hey, I'm important!" but what if conditionals become more complex?

Extract `doc.type == 'important'` to its own method, called `isImportant`.

```groovy
  changes
    .findAll { doc -> isImportant(doc) }
    // ...
    
  private boolean isImportant(doc) {
    doc.type == 'important'
  }
```

### "creating a resource"

Why do I care here how to invoke what method in a webservice? I just want to create a resource.

Extract all dealings with the webservice to it's own method, called `createResource`.

```groovy
  def resource = createResource(doc)
  // ...
    
  private Resource createResource(doc) {
    webservice.create(doc)
  }
```

### "update to processed"

Extract the details of associating resource/document/setting a status to its own method, called `updateToProcessed`.

```groovy
  updateToProcessed(doc, resource)
  // ...
    
  private void updateToProcessed(doc, resource) {
    doc.apiId = resource.id
    doc.status = 'processed'
  }
```

### "update to failed"

Don't care about the details. Extract to `updateToFailed`.

```groovy
  updateToFailed(doc, e)
  // ...
    
  private void updateToFailed(doc, e) {
    doc.status = 'failed'
    doc.error = e.message
  }
```

Seems that we're left with `documentDb.update(doc)` at the end. 

This is part of the storing of a processed/failed document in the database and I already described that on the highest level. 

I put it in each of the just created `updateTo*` methods - a lower level.

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

So, after extracting the details out, what's changed?

```groovy
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
```

Any human -- e.g. co-worker, your future self -- who would read this one out "aloud", would understand what's going from 30,000 ft. 

If you need the details of any of these steps, just drill down into the method.

Being able to write things declarative (previous part of this series) and telling a story on the right level (this part) will also help make future changes more easily in part 3 and beyond. 

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

--

_Follow me on [@tvinke](https://twitter.com/tvinke) if you like what you're reading or subscribe to my blog on [https://tedvinke.wordpress.com](https://tedvinke.wordpress.com)._