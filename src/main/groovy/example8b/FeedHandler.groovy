package example8b

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

//@CompileStatic
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