package example3optional

import spock.lang.Ignore
import spock.lang.Specification

@Ignore
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
    1 * webservice.create(_) >> Optional.of(([id: '7']))
    handler.handle(changes)

    then:
    1 * documentDb.update({
      it.status == 'processed' && it.apiId == '7'
    })

    when:
    1 * webservice.create(_) >> {
      Optional.empty()
    }
    handler.handle(changes)
    
    then:
    1 * documentDb.update({
      it.status == 'failed'
    })
  }
  
}
