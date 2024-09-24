import model.JsonArray;
import model.JsonObject;
import model.JsonValue;

import java.util.List;
import java.util.Map;

public class Json {

    private static final ObjectMapper mapper = new ObjectMapper();



    // Принимает на вход строку в Json формате -  возвращает Json объект из разобранной строки
    // Использует токенайзер, для последовательного получения элементов Json
    // Использует JsonObserver для выполнения семантических действий над JsonValue
    public static JsonObject parseJsonObject(String input) {
        JsonTokenizer tokenizer = new JsonTokenizer(input);
        JsonParser<JsonValue> parser = new JsonParser<>(tokenizer, new JsonObserver());
        return (JsonObject) parser.parse();
    }

    // Принимает на вход строку в Json формате -  возвращает Map -  где ключ - название строки, а value -  значение, лежащее в Json по этому названию
    // Разница с предыдущим методом только в том, что используется PlainObserver() - а в нем в стеке хранится как раз Map(строка, объект)
    public static Map<String, Object> parseObject(String input) {
        JsonTokenizer tokenizer = new JsonTokenizer(input);
        JsonParser<Object> parser = new JsonParser<>(tokenizer, new PlainObserver());
        return (Map<String, Object>) parser.parse();
    }



    // Read JSON string to List<>
    // Принимает на вход строку в Json формате -  возвращает List of JsonValue
    // Разница с parseObject - в том, что на стеке создастся не Map, a List
    public static List<Object> parseArray(String input) {
        JsonTokenizer tokenizer = new JsonTokenizer(input);
        JsonParser<Object> parser = new JsonParser<>(tokenizer, new PlainObserver());
        return (List<Object>) parser.parse();
    }

    // Read JSON string to JsonArray
    // Принимает на вход строку в Json формате -  возвращает List of JsonValue
    // Используется JsonObserver - а значит на стеке будет собираться объект Json

    public static JsonArray parseJsonArray(String input) {
        JsonTokenizer tokenizer = new JsonTokenizer(input);
        JsonParser<JsonValue> parser = new JsonParser<>(tokenizer, new JsonObserver());
        return (JsonArray) parser.parse();
    }


    // Использует функцию asProvided из ObjectMapper.
    //Преобразует входящую строку в требуемый класс, если это возможно
    public static <T> T parseObject(String input, Class<T> clazz) {
        return mapper.asProvided(parseJsonObject(input), clazz);
    }

    // Преобразует входящий объект некоторого класса в Json использует fromObject из Mapper
    public static JsonValue from(Object object) {
        return mapper.fromObject(object);
    }
}
