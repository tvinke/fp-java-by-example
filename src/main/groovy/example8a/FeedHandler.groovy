package example8a

import groovy.transform.Immutable
import io.vavr.control.Try

import java.util.function.BiFunction
import java.util.function.Function
import java.util.function.Predicate

@Immutable(copyWith = true)
class Doc {
  String title, type, apiId, status, error
}

class Resource extends HashMap {}

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