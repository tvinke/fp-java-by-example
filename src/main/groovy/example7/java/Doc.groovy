package example7.java

import groovy.transform.TupleConstructor

@TupleConstructor
class Doc {
  String title, type, apiId, status, error

  static Doc copyWith(Doc doc, Resource resource, status) {
    return new Doc(doc.title, doc.type, resource.get("id").toString(), status)
  }

  static Doc copyWith(Doc doc, status, Throwable t) {
    return new Doc(doc.title, doc.type, null, status, t.getMessage())
  }

  @Override
  String toString() {
    final StringBuilder sb = new StringBuilder("Doc{")
    sb.append("title='").append(title).append('\'')
    sb.append(", type='").append(type).append('\'')
    sb.append(", apiId='").append(apiId).append('\'')
    sb.append(", status='").append(status).append('\'')
    sb.append(", error='").append(error).append('\'')
    sb.append('}')
    return sb.toString()
  }
}


