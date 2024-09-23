# Json parser #

## Task: ##
### Implement a json parser with the following requirements: ###
- Do not use external libraries
- Read JSON string
    - To Java Object
    - To Map<String, Object>
    - *To specified class*
- Convert Java object to JSON string
- Library should support
    - Classes with fields (primitives, boxing types, null, arrays, classes)
    - Arrays
    - Collections
- Limitations (you may skip implementation)
    - Cyclic dependencies
    - non-representable in JSON types

## Structure of project ##

**Package model:**

Classes JsonArray, JsonLiteral, JsonNumber, JsonObject, JsonString - implementing the JsonValue interface.

Each of them represents a data type stored in JSON format.

**Json:** A class that contains all available methods of the implemented library.

**JsonToken:** An enumeration that contains all available tokens in a JSON object.

**JsonTokenizer:** A class that converts a string into a sequence of JSON tokens using regular expression matching. Implements the Iterator interface. The Next() method returns the next unprocessed token in the sequence.

**JsonParser:** A class that represents an LL(1) parser. Contains a lookupTable - a transition table representing the parsing rules for each terminal and non-terminal.

**JsonObserver:** A class that serves to handle semantic actions during the parsing of JSON data by the JsonParser. Uses a stack to build the final object.

**ObjectMapper:** A class that transforms JSON objects into Java objects and vice versa.


**Examples of using:**

In class Main.