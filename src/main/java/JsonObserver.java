import model.*;

import java.util.*;

class JsonObserver implements JsonParser.Observer<JsonValue> {

    private final Deque<JsonValue> stack = new ArrayDeque<>();

    @Override
    public JsonValue result() {
        return stack.pop();
    }

    @Override
    public void accept(JsonParser.Action action, JsonTokenizer tokenizer) {
        switch (action) {
                case AddElement -> {
                    var value = stack.pop();
                    ((JsonArray) stack.getFirst()).elements.add(value);
                }
                case CreateArray -> stack.push(new JsonArray(new ArrayList<>()));
                case CreateObject -> stack.push(new JsonObject(new LinkedHashMap<>()));
                case FalseSetValue -> stack.push(JsonLiteral.False);
                case NullSetValue -> stack.push(JsonLiteral.Null);
                case NumberSetValue -> stack.push(tokenizer.getNumber());
                case PutEntry -> {
                    var value = stack.pop();
                    var key = ((JsonString) stack.pop()).content;
                    (((JsonObject) stack.getFirst())).entries.put(key, value);
                }
                case StringSetValue -> stack.push(tokenizer.getString());
                case TrueSetValue -> stack.push(JsonLiteral.True);
                default -> throw new IllegalStateException("Unexpected action: " + this);
            }
    }
}
