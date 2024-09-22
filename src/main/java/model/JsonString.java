package model;

// Представляет строку в JSON
public class JsonString implements JsonValue {
    public final String content;

    public JsonString(String content) {
        this.content = content;
    }

    // реализовано в соответствии с https://ecma-international.org/wp-content/uploads/ECMA-404_2nd_edition_december_2017.pdf
    // раздел 9 String
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(content.length());
        builder.append('"');
        for (char c : content.toCharArray()) {
            switch (c) {
                case '\"' -> builder.append("\\\"");
                case '\\' -> builder.append("\\\\");
                case '\b' -> builder.append("\\b");
                case '\f' -> builder.append("\\f");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                default -> {
                    if (c <= '\u001F') {
                        builder.append("\\u%04x".formatted((int) c));
                        break;
                    }
                    builder.append(c);
                }
            }
        }
        builder.append('"');
        return builder.toString();
    }
}
