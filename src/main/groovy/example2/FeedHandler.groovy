package example2

class Doc {
  String title, type, apiId, status, error
}

class Resource extends HashMap {}

interface Webservice {
  Resource create(Doc doc) throws IOException
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