package example7.java;

import io.vavr.control.Try;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.stream.Collectors.toList;

import static io.vavr.API.$;
import static io.vavr.API.Match;
import static io.vavr.API.*;        // $, Case, Match
import static io.vavr.Predicates.*; // instanceOf

public class FeedHandlerJava {

  private Repository repository;

  public FeedHandlerJava(Repository repository) {
    this.repository = repository;
  }

  List<Doc> handle(List<Doc> changes,
    Function<Doc, Try<Resource>> creator) {

    return changes.stream()
      .filter(doc -> isImportant(doc))
      .map(doc -> {
        return creator.apply(doc)
          .recover(x -> Match(x).of(
            Case($(instanceOf(DuplicateResourceException.class)), t -> handleDuplicate(doc)),
            Case($(instanceOf(SpecialException.class)), t -> handleSpecial(t))
          )).map(resource -> {
            return setToProcessed(doc, resource);
          }).getOrElseGet(e -> {
            return setToFailed(doc, e);
          });
      }).collect(toList());
  }

  private Resource handleDuplicate(Doc alreadyProcessed) {
    // find earlier saved, existing resource and return that one
    return repository.findById(alreadyProcessed.getApiId());
  }

  private static Resource handleSpecial(SpecialException e) {
    // handle special situation
    return new Resource();
  }

  private static boolean isImportant(Doc doc) {
    return doc.getType().equals("important");
  }

  private static Doc setToProcessed(Doc doc, Resource resource) {
    return Doc.copyWith(doc, resource, "processed");
  }

  private static Doc setToFailed(Doc doc, Throwable e) {
    return Doc.copyWith(doc, "failed", e);
  }
}
