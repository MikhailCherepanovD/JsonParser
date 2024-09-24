import java.util.*;
import java.util.function.BiConsumer;

// LL-1 парсер JSON c семантическими действиями
class JsonParser<T> {
    enum Action {
        AddElement,  // добавляет элемент в массив
        CreateArray,  // создает массив и помещает в стек
        CreateObject,
        FalseSetValue, // помещает литерал null на вершину стека
        NullSetValue,
        NumberSetValue,
        PutEntry, // берет значение и ключ со стека и помещает в объект Json
        StringSetValue, //
        TrueSetValue; //
    }

    interface Observer<T> extends BiConsumer<Action, JsonTokenizer> {   // позволяет принимать два аргумента и не возвращать значений
        T result();
    }

    private final JsonTokenizer tokenizer;
    private final Observer<T> observer;// тут может быть либо JsonObserver или plainObserver

    /*
     * Value -> beginObject Object endObject | beginArray Array endArray | string | number | null | false | true
     * Object -> string colon Value Members | ''
     * Members -> '' | comma string colon Value Members
     * Array -> Value Elements | ''
     * Elements -> '' | comma Value Elements
     *
     * Таблица парсинга: https://mdaines.github.io/grammophone/?s=VmFsdWUgLT4gYmVnaW5PYmplY3QgT2JqZWN0IGVuZE9iamVjdCB8IGJlZ2luQXJyYXkgQXJyYXkgZW5kQXJyYXkgfCBzdHJpbmcgfCBudW1iZXIgfCBudWxsIHwgZmFsc2UgfCB0cnVlLgpPYmplY3QgLT4gc3RyaW5nIGNvbG9uIFZhbHVlIE1lbWJlcnMgfCAuCk1lbWJlcnMgLT4gIHwgY29tbWEgc3RyaW5nIGNvbG9uIFZhbHVlIE1lbWJlcnMuCkFycmF5IC0+IFZhbHVlIEVsZW1lbnRzIHwgLgpFbGVtZW50cyAtPiAgfCBjb21tYSBWYWx1ZSBFbGVtZW50cy4=
     * */
    private enum NonTerminal {
        Value,
        Object,
        Members,
        Array,
        Elements
    }

    private static final JsonToken END = null;

    // implements Comparable<LookupKey> - значит, что объекты этого класса можно сравнивать
    private static class LookupKey implements Comparable<LookupKey> {
        NonTerminal nonTerminal;
        JsonToken terminal;

        private LookupKey(NonTerminal nonTerminal, JsonToken terminal) {
            this.nonTerminal = nonTerminal;
            this.terminal = terminal;
        }

        private LookupKey() {
        }

        @Override
        public int compareTo(LookupKey other) {
            int x = nonTerminal.compareTo(other.nonTerminal);
            if (x == 0) {
                if (other.terminal == END) {
                    return terminal != END ? 1 : 0;
                }
                return terminal.compareTo(other.terminal);
            }
            return x;
        }

    }
    //
    private static final Map<LookupKey, List<Object>> lookupTable;

    //создание lookup table
    static {
        Map<LookupKey, List<Object>> m = new TreeMap<>();
        m.put(new LookupKey(NonTerminal.Value, JsonToken.ObjectBegin), List.of(
                Action.CreateObject, JsonToken.ObjectBegin, NonTerminal.Object, JsonToken.ObjectEnd
        ));
        m.put(new LookupKey(NonTerminal.Value, JsonToken.ArrayBegin), List.of(
                Action.CreateArray, JsonToken.ArrayBegin, NonTerminal.Array, JsonToken.ArrayEnd
        ));
        m.put(new LookupKey(NonTerminal.Value, JsonToken.String), List.of(JsonToken.String, Action.StringSetValue));
        m.put(new LookupKey(NonTerminal.Value, JsonToken.Number), List.of(JsonToken.Number, Action.NumberSetValue));
        m.put(new LookupKey(NonTerminal.Value, JsonToken.True), List.of(JsonToken.True, Action.TrueSetValue));
        m.put(new LookupKey(NonTerminal.Value, JsonToken.False), List.of(JsonToken.False, Action.FalseSetValue));
        m.put(new LookupKey(NonTerminal.Value, JsonToken.Null), List.of(JsonToken.Null, Action.NullSetValue));

        m.put(new LookupKey(NonTerminal.Object, JsonToken.ObjectEnd), List.of());
        m.put(new LookupKey(NonTerminal.Object, JsonToken.String), List.of(
                JsonToken.String, Action.StringSetValue, JsonToken.Colon, NonTerminal.Value, Action.PutEntry,
                NonTerminal.Members
        ));
        m.put(new LookupKey(NonTerminal.Members, JsonToken.ObjectEnd), List.of());
        m.put(new LookupKey(NonTerminal.Members, JsonToken.Comma), List.of(
                JsonToken.Comma, JsonToken.String, Action.StringSetValue, JsonToken.Colon, NonTerminal.Value, Action.PutEntry,
                NonTerminal.Members
        ));
        List<Object> arrayElems = List.of(NonTerminal.Value, Action.AddElement, NonTerminal.Elements);
        m.put(new LookupKey(NonTerminal.Array, JsonToken.ObjectBegin), arrayElems);
        m.put(new LookupKey(NonTerminal.Array, JsonToken.ArrayBegin), arrayElems);
        m.put(new LookupKey(NonTerminal.Array, JsonToken.String), arrayElems);
        m.put(new LookupKey(NonTerminal.Array, JsonToken.Number), arrayElems);
        m.put(new LookupKey(NonTerminal.Array, JsonToken.Null), arrayElems);
        m.put(new LookupKey(NonTerminal.Array, JsonToken.False), arrayElems);
        m.put(new LookupKey(NonTerminal.Array, JsonToken.True), arrayElems);
        m.put(new LookupKey(NonTerminal.Array, JsonToken.ArrayEnd), List.of());

        m.put(new LookupKey(NonTerminal.Elements, JsonToken.ArrayEnd), List.of());
        m.put(new LookupKey(NonTerminal.Elements, JsonToken.Comma), List.of(
                JsonToken.Comma, NonTerminal.Value, Action.AddElement, NonTerminal.Elements
        ));
        lookupTable = Collections.unmodifiableMap(m);
    }

    JsonParser(JsonTokenizer tokenizer, Observer<T> observer) {
        this.tokenizer = tokenizer;
        this.observer = observer;
    }

    T parse() {

        Deque<Object> stack = new ArrayDeque<>();
        //В стеке хронятся либо терминалы, либо нетерминалы, либо действия
        var key = new LookupKey(); // ключ для поиска в lookUp table

        stack.push(JsonToken.End); //
        stack.push(NonTerminal.Value); //
        JsonToken token = tokenizer.next(); // ObjectBegin

        while (!stack.isEmpty() && tokenizer.hasNext()) {
            //Если верхний элемент стека — это действие (экземпляр Action), оно принимается наблюдателем, и элемент удаляется из стека.
            if (stack.getFirst() instanceof Action action) {
                observer.accept(action, tokenizer);
                stack.pop();
                continue;
            }
            //Если текущий токен совпадает с верхним элементом стека, верхний элемент удаляется, и берется следующий токен из токенизатора.
            if (token == stack.getFirst()) {  // stack.getFirst() -
                stack.pop();
                token = tokenizer.next();
                continue;
            }
            //Если верхний элемент стека — это токен (не действие и не нетерминал), выбрасывается исключение, указывающее на неожиданный токен.
            if (stack.getFirst() instanceof JsonToken topToken) {
                throw new JsonException("Unexpected token " + topToken);
            }
            key.nonTerminal = ((NonTerminal) stack.pop());
            key.terminal = token;
            List<Object> rightHandSide = lookupTable.get(key);
            if (rightHandSide == null) {
                throw new JsonException("Unexpected token " + token);
            }
            for (var symbol : rightHandSide.reversed()) {
                stack.push(symbol);// кладем правую часть продукции в обратном порядке
            }
        }
        return observer.result();
    }
}
