import model.*;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.*;

// Класс, дающий возможность транслировать POJO в классы-наследники JsonValue и наоборот
class ObjectMapper {

    private Object asOrdinary(JsonValue json) {
        return switch (json) {
            case JsonObject o -> asOrdinaryMap(o);
            case JsonArray array -> asOrdinaryList(array);
            case JsonLiteral literal -> literal;
            case JsonNumber number -> number.value;
            case JsonString string -> string.content;
            default -> throw new IllegalStateException("Unexpected value: " + json);
        };
    }

    private List<Object> asOrdinaryList(JsonArray array) {
        List<Object> list = new ArrayList<>();
        for (JsonValue element : array.elements) {
            Object ordinary = asOrdinary(element);
            list.add(ordinary);
        }
        return list;
    }

    public Map<String, Object> asOrdinaryMap(JsonObject o) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        for (Map.Entry<String, JsonValue> e : o.entries.entrySet()) {
            map.merge(e.getKey(), asOrdinary(e.getValue()), (x, y) -> {
                // all keys must be different in o.keyValues
                throw new IllegalStateException();
            });
        }
        return map;
    }


    public <T> JsonValue fromObject(T obj) {
        switch (obj) {
            case null -> {
                return JsonLiteral.Null;
            }
            case Boolean value -> {
                return value ? JsonLiteral.True : JsonLiteral.False;
            }
            case BigDecimal value -> {
                return new JsonNumber(value);
            }
            case Byte value -> {
                return new JsonNumber(value);
            }
            case Short value -> {
                return new JsonNumber(value);
            }
            case Integer value -> {
                return new JsonNumber(value);
            }
            case Long value -> {
                return new JsonNumber(value);
            }
            case Float value -> {
                return new JsonNumber(value);
            }
            case Double value -> {
                return new JsonNumber(value);
            }
            case String value -> {
                return new JsonString(value);
            }
            default -> {}
        }
        var clazz = obj.getClass();
        if (obj instanceof Object[] array) {
            List<JsonValue> list = new ArrayList<>();
            for (Object o : array) {
                JsonValue jsonValue = fromObject(o);
                list.add(jsonValue);
            }
            return new JsonArray(list);
        }
        if (clazz.isArray()) {
            int length = Array.getLength(obj);
            final Object[] result = new Object[length];
            for(int i = 0; i < length; i++)
                result[i] = Array.get(obj, i);
            return fromObject(result);
        }
        if (obj instanceof Collection<?> collection) {
            List<JsonValue> list = new ArrayList<>();
            for (Object o : collection) {
                JsonValue jsonValue = fromObject(o);
                list.add(jsonValue);
            }
            return new JsonArray(list);
        }
        if (obj instanceof Map<?,?> map) {
            Map<String, JsonValue> result = new HashMap<>();
            for (Map.Entry<String, Object> e : ((Map<String, Object>) map).entrySet()) {
                result.merge(e.getKey(), fromObject(e.getValue()), (x, y) -> {
                    // так обеспечим отстуствие повторов ключей
                    throw new IllegalStateException();
                });
            }
            return new JsonObject(result);
        }
        return fromPlainObject(obj);
    }

    public <T> T asProvided(JsonValue source, Class<T> clazz) {
        return switch (source) {
            case JsonLiteral.Null -> {
                yield null;
            }
            case JsonLiteral.True -> {
                if (!Boolean.TYPE.equals(clazz)){
                    throw new RuntimeException("is not boolean");
                }
                yield (T) Boolean.TRUE;
            }
            case JsonLiteral.False -> {
                if (!Boolean.TYPE.equals(clazz)){
                    throw new RuntimeException("is not boolean");
                }
                yield (T) Boolean.FALSE;
            }
            case JsonNumber number -> {
                BigDecimal value;
                switch (number.value) {
                    case Integer x -> value = BigDecimal.valueOf(x);
                    case Long x -> value = BigDecimal.valueOf(x);
                    case Double x -> value = BigDecimal.valueOf(x);
                    case BigDecimal x -> value = x;
                    default -> throw new IllegalArgumentException("Unexpected Json Number Type: " + number.value.getClass());
                }
                if (BigDecimal.class.equals(clazz)) {
                    yield (T)value;
                }
                if (Byte.TYPE.equals(clazz)) {
                    yield (T)Byte.valueOf(value.byteValueExact());
                }
                if (Short.TYPE.equals(clazz)) {
                    yield (T)Short.valueOf(value.shortValueExact());
                }
                if (Integer.TYPE.equals(clazz)) {
                    yield (T)Integer.valueOf(value.intValueExact());
                }
                if (Long.TYPE.equals(clazz)) {
                    yield (T)Long.valueOf(value.longValueExact());
                }
                if (Float.TYPE.equals(clazz)) {
                    yield (T)Float.valueOf(value.floatValue());
                }
                if (Double.TYPE.equals(clazz)) {
                    yield (T)Double.valueOf(value.doubleValue());
                }
                throw new JsonException(clazz + " is not numeric type");
            }
            case JsonString string -> {
                if (String.class.equals(clazz)) {
                    yield (T) string.content;
                }
                throw new JsonException(clazz + " is not a string type");
            }
            case JsonObject object -> asProvidedObject(object, clazz);
            case JsonArray array -> asProvidedArray(array, clazz);
            default -> throw new IllegalStateException("Unexpected value: " + source);
        };
    }

    private <T> T asProvidedArray(JsonArray array, Class<T> clazz) {
        int size = array.elements.size();
        final var values = array.elements;
        if (clazz.isArray()) {
            Class<?> componentType = clazz.getComponentType();
            var result = Array.newInstance(componentType, size);
            for (int i = 0; i < size; i++) {
                Array.set(result, i, asProvided(values.get(i), componentType));
            }
            return (T) result;
        }
        if (!Collection.class.isAssignableFrom(clazz)) {
            throw new JsonException(clazz + " is not a collection");
        }
        try {
            T result;
            if (clazz.isAssignableFrom(List.class)) {
                result = (T) new ArrayList();
            } else {
                result = clazz.getDeclaredConstructor().newInstance();
            }
            var addMethod = clazz.getMethod("add", Object.class);
            for (JsonValue value : values) {
                // use 'ordinary' as a hack because in other case we need more information
                // on type parameter of a parametrized collection.
                addMethod.invoke(result, asProvided(value, addMethod.getParameterTypes()[0]));
            }
            return result;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private <T> T asProvidedObject(JsonObject object, Class<T> clazz) {
        try {
            T result;
            if (Map.class.isAssignableFrom(clazz)) {
                result = (T) new LinkedHashMap();
                // use 'ordinary' as a hack here too
                clazz.getMethod("putAll", Map.class).invoke(result, asOrdinaryMap(object));
                return result;
            }
            result = clazz.getConstructor().newInstance();
            for (var entry : object.entries.entrySet()) {
                final Field field = clazz.getDeclaredField(entry.getKey());
                field.setAccessible(true);
                field.set(result, asProvided(entry.getValue(), field.getType()));
            }
            return result;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }


    private JsonObject fromPlainObject(Object obj) {
        var clazz = obj.getClass();
        var result = new JsonObject(new HashMap<>());
        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            String key = field.getName();
            try {
                JsonValue value = fromObject(field.get(obj));
                result.entries.put(key, value);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        return result;
    }
}
