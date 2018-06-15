package example3

import spock.lang.Specification

import java.util.concurrent.CompletableFuture

class FeedHandlerSpec extends Specification {
  
  void setup() {
  }
  
  def "should handle changes"() {
    
    given:
    def webservice = Mock(Webservice)
    def documentDb = Mock(DocumentDb)
    def handler = new FeedHandler(webservice: webservice, documentDb: documentDb)
    def changes = [
      new Doc([title: 'Groovy', type: 'important']),
      new Doc([title: 'Ruby', type: 'minor']),
    ]
    
    when:
    1 * webservice.create(_) >> new CompletableFuture([id: '7'])
    handler.handle(changes)
    
    then:
    1 * documentDb.update({
      it.status == 'processed' && it.apiId == '7'
    })
    
    when:
    1 * webservice.create(_) >> {
      new CompletableFuture(new Resource())
        .thenApply {
        throw new RuntimeException()
      }
    }
    handler.handle(changes)
    
    then:
    1 * documentDb.update({
      it.status == 'failed'
    })
  }
  
}
