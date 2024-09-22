package model;

import java.util.Map;
import java.util.StringJoiner;

public class JsonObject implements JsonValue {
    public final Map<String, JsonValue> entries;

    public JsonObject(Map<String, JsonValue> entries) {
        this.entries = entries;
    }

    @Override
    public java.lang.String toString() {
        final var joiner = new StringJoiner(", ", "{", "}");
        for (var entry : entries.entrySet()) {
            joiner.add("\"%s\": %s".formatted(entry.getKey(), entry.getValue()));
        }
        return joiner.toString();
    }
}
