import model.JsonArray;
import model.JsonObject;
import model.JsonValue;

import java.util.Map;

public class Json {

    private static final ObjectMapper mapper = new ObjectMapper();

    // Read JSON string to JsonArray
    public static JsonArray parseArray(String input) {
        JsonTokenizer tokenizer = new JsonTokenizer(input);
        JsonParser<JsonValue> parser = new JsonParser<>(tokenizer, new JsonObserver());
        return (JsonArray) parser.parse();
    }

    // Read JSON string to Java object
    public static JsonObject parseJsonObject(String input) {
        JsonTokenizer tokenizer = new JsonTokenizer(input);
        JsonParser<JsonValue> parser = new JsonParser<>(tokenizer, new JsonObserver());
        return (JsonObject) parser.parse();
    }

    // Read JSON string to Map<String, Object>
    public static Map<String, Object> parseObject(String input) {
        JsonTokenizer tokenizer = new JsonTokenizer(input);
        JsonParser<Object> parser = new JsonParser<>(tokenizer, new PlainObserver());
        return (Map<String, Object>) parser.parse();
    }

    // Read JSON string to specified class
    public static <T> T parseObject(String input, Class<T> clazz) {
        return mapper.asProvided(parseJsonObject(input), clazz);
    }

    // Convert Java object to JSON string (via toString of JsonValue)
    public static JsonValue from(Object object) {
        return mapper.fromObject(object);
    }
}
