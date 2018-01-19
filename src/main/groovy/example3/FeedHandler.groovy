package example3

import java.util.concurrent.CompletableFuture

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