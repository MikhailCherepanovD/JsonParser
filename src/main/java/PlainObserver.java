import model.JsonArray;
import model.JsonLiteral;
import model.JsonObject;
import model.JsonString;

import java.util.*;

// Вспомогательный класс, расширяющий возможности JsonParser
    //реализует выполнение семантических действий в процессе разбора последовательности токенов
class PlainObserver implements JsonParser.Observer<Object> {
    // использует стандартные java объекты
    private final Stack<Object> stack = new Stack<>();

    @Override
    public Object result() {
        return stack.pop();            // в конце в результате выполнения должен остаться один объект
    }
    //
    @Override
    public void accept(JsonParser.Action action, JsonTokenizer tokenizer) {
        switch (action) {
            case AddElement -> {
                var value = stack.pop();
                ((List<Object>) stack.peek()).add(value);
            }
            case CreateArray -> stack.push(new ArrayList<>());
            case CreateObject -> stack.push(new LinkedHashMap<>());
            case FalseSetValue -> stack.push(false);
            case NullSetValue -> stack.push(null);
            case NumberSetValue -> stack.push(tokenizer.getNumber().value);

            case PutEntry -> {
                var value = stack.pop();
                var key = (String) stack.pop();
                // собираем Json из объектов на стеке
                ((Map<String, Object>) stack.peek()).put(key, value);
            }
            case StringSetValue -> stack.push(tokenizer.getString().content);
            case TrueSetValue -> stack.push(true);
            default -> throw new IllegalStateException("Unexpected action: " + this);
        }
    }
}
