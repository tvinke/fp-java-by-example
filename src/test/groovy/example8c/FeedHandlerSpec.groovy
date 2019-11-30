package example8c

import io.vavr.control.Either
import spock.lang.Specification

class FeedHandlerSpec extends Specification {

  void setup() {
  }


  def "should handle changes"() {

    given:
    def handler = new FeedHandler()
    def changes = [
      new Doc([title: 'Groovy', type: 'important']),
      new Doc([title: 'Ruby', type: 'minor']),
    ]
    def successCreator = { document ->
      Either.right(
        new CreationSuccess(
          doc: document,
          resource: [id: '7']
        )
      )
    }

    when:
    def docs = handler.handle(changes, successCreator)

    then:
    def doc = docs.first()
    doc.status == 'processed'
    doc.apiId == '7'

    when:
    def failureCreator = { document ->
      Either.left(
        new CreationFailed(
          doc: document,
          e: new RuntimeException()
        )
      )
    }
    handler = new FeedHandler()
    docs = handler.handle(changes, failureCreator)

    then:
    docs[0].status == 'failed'
  }

}
