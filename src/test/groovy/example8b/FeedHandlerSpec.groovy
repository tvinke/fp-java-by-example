package example8b

import io.vavr.control.Try
import spock.lang.Specification

import java.util.function.BiFunction

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
    def creator = { Try.of { [id: '7'] } }
    
    when:
    def docs = handler.handle(changes, creator)
    
    then:
    def doc = docs.first()
    doc.status == 'processed'
    doc.apiId == '7'
    
    when:
    creator = {
      Try.ofCallable {
        throw new RuntimeException()
      }
    }
    handler = new FeedHandler()
    docs = handler.handle(changes, creator)
    
    then:
    docs[0].status == 'failed'

    when:
    BiFunction<Doc, Throwable, Doc> customFailureMapper = { failingDoc, e ->
      failingDoc.copyWith(
        status: 'my-custom-fail-status',
        error: e.message
      )
    }
    handler = new FeedHandler()
    docs = handler.handle(changes,
      creator,
      FeedHandler.DEFAULT_FILTER,
      FeedHandler.DEFAULT_SUCCESS_MAPPER,
      customFailureMapper
    )

    then:
    docs[0].status == 'my-custom-fail-status'
  }
  
}
