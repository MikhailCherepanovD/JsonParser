package model;

import java.util.List;
import java.util.StringJoiner;

// Представляет массив в JSON
public class JsonArray implements JsonValue {
    public final List<JsonValue> elements;

    public JsonArray(List<JsonValue> elements) {
        this.elements = elements;
    }
    public List<JsonValue> getListOfJsonValues(){
        return elements;
    }
    @Override
    public String toString() {
        final var joiner = new StringJoiner(", ", "[", "]");
        for (JsonValue element : elements) {
            joiner.add(element.toString());
        }
        return joiner.toString();
    }
}
