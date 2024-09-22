package model;

public enum JsonLiteral implements JsonValue {
    True, False, Null;

    @Override
    public String toString() {
        return switch (this) {
            case True -> "true";
            case False -> "false";
            case Null -> "null";
        };
    }
}
