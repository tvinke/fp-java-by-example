package example4

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

interface DocumentDb {
  void update(obj) throws IOException
}

class FeedHandler {

  Webservice webservice
  DocumentDb documentDb

  void handle(List<Doc> changes) {

    changes
      .findAll { doc -> isImportant(doc) }
      .each { doc ->
        createResource(doc)
        .thenAccept { resource ->
          documentDb.update(
            setToProcessed(doc, resource)
          )
        }
        .exceptionally { e ->
          documentDb.update(setToFailed(doc, e))
        }
      }
  }

  private CompletableFuture<Resource> createResource(doc) {
    webservice.create(doc)
  }
  
  private boolean isImportant(doc) {
    doc.type == 'important'
  }
  
  private Doc setToProcessed(doc, resource) {
    doc.copyWith(
      status: 'processed',
      apiId: resource.id
    )
  }
  
  private Doc setToFailed(doc, e) {
    doc.copyWith(
      status: 'failed',
      error: e.message
    )
  }

}