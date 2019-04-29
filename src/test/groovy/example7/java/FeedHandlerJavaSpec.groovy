package example7.java


import io.vavr.control.Try
import spock.lang.Specification

class FeedHandlerJavaSpec extends Specification {
  
  void setup() {
  }
  
  def "should handle changes"() {
    
    given:
    def handler = new FeedHandlerJava()
    def changes = [
            new Doc([title: 'Groovy', type: 'important']),
            new Doc([title: 'Ruby', type: 'minor']),
    ]
    def creator = { Try.of { [id: '7'] as Resource } }
    
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
    handler = new FeedHandlerJava()
    docs = handler.handle(changes, creator)
    
    then:
    docs[0].status == 'failed'
  }

  def "should handle duplicate"() {

    given:
    def repository = Mock(Repository) {
      findById(_) >> ([id: '7'] as Resource)
    }

    and:
    def handler = new FeedHandlerJava(repository)
    def changes = [
      new Doc([title: 'Groovy', type: 'important']),
      new Doc([title: 'Ruby', type: 'minor']),
    ]
    def creator = {
      Try.ofCallable {
        throw new DuplicateResourceException()
      }
    }

    when:
    def docs = handler.handle(changes, creator)

    then:
    def doc = docs.first()
    doc.status == 'processed'
    doc.apiId == '7'
  }
  
}
