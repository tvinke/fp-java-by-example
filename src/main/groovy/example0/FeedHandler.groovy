package example0

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
