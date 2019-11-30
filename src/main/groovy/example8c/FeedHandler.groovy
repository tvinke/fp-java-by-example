package example8c

import groovy.transform.Immutable
import io.vavr.control.Either

import java.util.function.Function
import java.util.function.Predicate

@Immutable(copyWith = true)
class Doc {
  String title, type, apiId, status, error
}

class Resource extends HashMap {}

class CreationSuccess {
  Doc doc
  Resource resource
}

class CreationFailed {
  Doc doc
  Exception e
}

//@CompileStatic
class FeedHandler {

  private static final Predicate<Doc> DEFAULT_FILTER = { doc ->
    doc.type == 'important'
  }

  private static final Function<CreationSuccess, Doc> DEFAULT_SUCCESS_MAPPER = { success ->
    success.doc.copyWith(
      status: 'processed',
      apiId: success.resource.id
    )
  }

  private static final Function<CreationFailed, Doc> DEFAULT_FAILURE_MAPPER = { failed ->
    failed.doc.copyWith(
      status: 'failed',
      error: failed.e.message
    )
  }
  
  List<Doc> handle(List<Doc> changes,
                   Function<Doc, Either<CreationFailed, CreationSuccess>> creator,
                   Predicate<Doc> filter = DEFAULT_FILTER,
                   Function<CreationSuccess, Doc> successMapper = DEFAULT_SUCCESS_MAPPER,
                   Function<CreationFailed, Doc> failureMapper = DEFAULT_FAILURE_MAPPER) {

    changes
      .findAll { filter }
      .collect { doc ->
        creator.apply(doc)
        .map(successMapper)
        .getOrElseGet(failureMapper)
      }
  }

}