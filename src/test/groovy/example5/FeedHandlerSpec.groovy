package example5

import spock.lang.Specification

import java.util.concurrent.CompletableFuture

class FeedHandlerSpec extends Specification {
  
  void setup() {
  }
  
  def "should handle changes"() {
    
    given:
    def webservice = Mock(Webservice)
    def handler = new FeedHandler(webservice: webservice)
    def changes = [
      new Doc([title: 'Groovy', type: 'important']),
      new Doc([title: 'Ruby', type: 'minor']),
    ]
    
    when:
    1 * webservice.create(_) >> new CompletableFuture([id: '7'])
    def docs = handler.handle(changes)
    
    then:
    def doc = docs.first()
    doc.status == 'processed'
    doc.apiId == '7'
    
    when:
    1 * webservice.create(_) >> {
      new CompletableFuture(new Resource())
        .thenApply {
        throw new RuntimeException()
      }
    }
    docs = handler.handle(changes)
    
    then:
    docs[0].status == 'failed'
  }
  
}
