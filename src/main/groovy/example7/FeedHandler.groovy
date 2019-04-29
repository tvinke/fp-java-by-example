package example7

import example7.java.DuplicateResourceException
import example7.java.SpecialException
import groovy.transform.Immutable
import io.vavr.control.Try
import static io.vavr.API.*;        // $, Case, Match
import static io.vavr.Predicates.*; // instanceOf

import java.util.function.Function

@Immutable(copyWith = true)
class Doc {
  String title, type, apiId, status, error
}

class Resource extends HashMap {}

class FeedHandler {
  
  List<Doc> handle(List<Doc> changes,
    Function<Doc, Try<Resource>> creator) {

    changes
      .findAll { doc -> isImportant(doc) }
      .collect { doc ->
        creator.apply(doc)
        .recover { x ->
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