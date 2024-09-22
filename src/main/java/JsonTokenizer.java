import model.JsonNumber;
import model.JsonString;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Лексический анализатор JSON
class JsonTokenizer implements Iterator<JsonToken> {

    private final Matcher matcher;

    private static final Pattern TOKEN = Pattern.compile("""
            \\s*    # пробелы -> игнорим
            (# группа #1
             # STRING
             "((?: \\\\[\\"\\\\/bfnrt]     # escaped character # группа #2
                  | \\\\u[a-fA-F0-9]{4}  # escaped unicode
                  | [^"\\\\]             # character
                )*)
             "
             # NUMBER
             | ( [+-]?   # группа #3
               (?:\\d+\\.?\\d* | \\.\\d+)  # обычное число
               (?:[eE][+-]?\\d+)?          # экспоненциальная форма числа
               )
             | true
             | false
             | null
             | \\{   # OBJECT_BEGIN
             | }     # OBJECT_END
             | \\[   # ARRAY_BEGIN
             | ]     # ARRAY_END
             | :     # COLON
             | ,     # COMMA
            )""", Pattern.COMMENTS);

    private static final Pattern CHARACTER = Pattern.compile("""
            (?: 
              ([^"\\\\])             # character          # группа #1
            | \\\\u([a-fA-F0-9]{4})  # escaped unicode    # группа #2
            | \\\\(["\\\\/bfnrt])     # escaped character # группа #3
            )""", Pattern.COMMENTS);

    public JsonTokenizer(String input) {
        this.matcher = TOKEN.matcher(input);
    }

    String contentsCoded;
    JsonString getString() {
        StringBuilder result = new StringBuilder(contentsCoded.length());
        Matcher matcherChar = CHARACTER.matcher(contentsCoded);
        while (matcherChar.find()) {
            if (matcherChar.group(1) != null) {
                result.append(matcherChar.group(1));
                continue;
            }
            if (matcherChar.group(2) != null) {
                result.append(Integer.valueOf(matcherChar.group(2), 16));
                continue;
            }
            assert matcherChar.group(3) != null;
            switch (matcherChar.group(3).charAt(0)) {
                case '\\' -> result.append('\\');
                case '/' -> result.append('/');
                case 'b' -> result.append('\b');
                case 'f' -> result.append('\f');
                case 'n' -> result.append('\n');
                case 'r' -> result.append('\r');
                case 't' -> result.append('\t');
            }
        }
        return new JsonString(result.toString());
    }

    String num;

    JsonNumber getNumber() {
        try {
            int integer = Integer.parseInt(num);
            return new JsonNumber(integer);
        } catch (NumberFormatException ignored) {
        }
        try {
            long longInt = Long.parseLong(num);
            return new JsonNumber(longInt);
        } catch (NumberFormatException ignored) {
        }
        try {
            double doubleNumber = Double.parseDouble(num);
            return new JsonNumber(doubleNumber);
        } catch (NumberFormatException ignored) {
        }
        try {
            BigDecimal big = new BigDecimal(num);
            return new JsonNumber(big);
        } catch (NumberFormatException e) {
            throw new JsonException("could not parse num=" + num, e);
        }
    }

    @Override
    public boolean hasNext() {
        return !matcher.hitEnd();
    }

    @Override
    public JsonToken next() {
            if (!matcher.find()) {
                return JsonToken.End;
            }
            switch (matcher.group(1)) {
                case "{":
                    return JsonToken.ObjectBegin;
                case "}":
                    return JsonToken.ObjectEnd;
                case "[":
                    return JsonToken.ArrayBegin;
                case "]":
                    return JsonToken.ArrayEnd;
                case ":":
                    return JsonToken.Colon;
                case ",":
                    return JsonToken.Comma;
                case "true":
                    return JsonToken.True;
                case "false":
                    return JsonToken.False;
                case "null":
                    return JsonToken.Null;
            }
            char first = matcher.group(1).charAt(0);
            switch (first) {
                case '"':
                    contentsCoded = matcher.group(2);
                    return JsonToken.String;
                case '+':
                case '-':
                    num = matcher.group(3);
                    return JsonToken.Number;
            }
            if (Character.isDigit(first)) {
                num = matcher.group(3);
                return JsonToken.Number;
            }

            throw new JsonException("input contains unrecognized token");

    }
}
