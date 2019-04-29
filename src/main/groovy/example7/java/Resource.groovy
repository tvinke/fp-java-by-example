package example7.java

import groovy.transform.InheritConstructors

class Resource extends HashMap {}

interface Repository {
  Resource findById(String id)
}

@InheritConstructors
class DuplicateResourceException extends RuntimeException {}

@InheritConstructors
class SpecialException extends RuntimeException {}

