package example5

import groovy.transform.Immutable

import java.util.concurrent.CompletableFuture

@Immutable(copyWith = true)
class Doc {
  String title, type, apiId, status, error
}

class Resource extends HashMap {}

interface Webservice {
  CompletableFuture<Resource> create(Doc doc)
}

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