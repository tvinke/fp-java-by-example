package example7

import io.vavr.control.Try
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
  }
  
}
