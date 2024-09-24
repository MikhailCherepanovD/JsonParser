import model.*;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.*;

// Класс, дающий возможность транслировать POJO в классы-наследники JsonValue и наоборот
class ObjectMapper {

    //рекурсивная функция, которая с пимощью "Поиска в глубину" приводит объект определенного класса к jsonValue,
    //+
    public <T> JsonValue fromObject(T obj) {
        //Проверка на примитивные типя
        switch (obj) {
            case null:
                return JsonLiteral.Null;
            case Boolean value:
                return value ? JsonLiteral.True : JsonLiteral.False;
            case BigDecimal value:
                return new JsonNumber(value);
            case Byte value:
                return new JsonNumber(value);
            case Short value:
                return new JsonNumber(value);
            case Integer value:
                return new JsonNumber(value);
            case Long value:
                return new JsonNumber(value);
            case Float value:
                return new JsonNumber(value);
            case Double value:
                return new JsonNumber(value);
            case String value:
                return new JsonString(value);
            default:
                break;
        }
        var clazz = obj.getClass();

        // тут происходит обработка массива пользовательского типа
        if (obj instanceof Object[] array) {
            List<JsonValue> list = new ArrayList<>();
            for (Object o : array) {
                JsonValue jsonValue = fromObject(o);
                list.add(jsonValue);
            }
            return new JsonArray(list);
        }
        // обработка всех других типов массива
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

    //Преобразование из POJO
    private JsonObject fromPlainObject(Object obj) {
        var clazz = obj.getClass();
        var result = new JsonObject(new HashMap<>());// Создаётся новый экземпляр JsonObject, который будет содержать пары ключ-значение

        for (Field field : clazz.getDeclaredFields()) { // перечисляются все поля класса
            field.setAccessible(true); // Открывается доступ к полю, чтобы можно было его прочитать, даже если оно является приватным.
            String key = field.getName();  //Извлекается имя текущего поля, которое будет использоваться в качестве ключа в JsonObject.

            try {
                JsonValue value = fromObject(field.get(obj)); // продолжаем рекурсивно разбирать объект
                result.entries.put(key, value);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        return result;
    }



    //рекурсивная функция, которая с пимощью "Поиска в глубину" приводит объект JsonValue к определенному классу,
    // и если поля Json и класса  не совпадают - выкидывает исключение
    //+
    public <T> T asProvided(JsonValue source, Class<T> clazz) {
        // сюда приходит один из классов, наследуемый от JsonValue
        switch (source) {
            case JsonLiteral.Null:
                return null;
            case JsonLiteral.True:
                if (!Boolean.TYPE.equals(clazz)) {
                    throw new RuntimeException("is not boolean");
                }
                return (T) Boolean.TRUE;
            case JsonLiteral.False:
                if (!Boolean.TYPE.equals(clazz)) {
                    throw new RuntimeException("is not boolean");
                }
                return (T) Boolean.FALSE;
            case JsonNumber number:
                BigDecimal value;
                switch (number.value) {
                    case Integer x -> value = BigDecimal.valueOf(x);
                    case Long x -> value = BigDecimal.valueOf(x);
                    case Double x -> value = BigDecimal.valueOf(x);
                    case BigDecimal x -> value = x;
                    default ->
                            throw new IllegalArgumentException("Unexpected Json Number Type: " + number.value.getClass());
                }
                if (BigDecimal.class.equals(clazz)) {
                    return (T) value;
                }
                if (Byte.TYPE.equals(clazz)) {
                    return (T) Byte.valueOf(value.byteValueExact());
                }
                if (Short.TYPE.equals(clazz)) {
                    return (T) Short.valueOf(value.shortValueExact());
                }
                if (Integer.TYPE.equals(clazz)) {
                    return (T) Integer.valueOf(value.intValueExact());
                }
                if (Long.TYPE.equals(clazz)) {
                    return (T) Long.valueOf(value.longValueExact());
                }
                if (Float.TYPE.equals(clazz)) {
                    return (T) Float.valueOf(value.floatValue());
                }
                if (Double.TYPE.equals(clazz)) {
                    return (T) Double.valueOf(value.doubleValue());
                }
                throw new JsonException(clazz + " is not numeric type");
            case JsonString string:
                if (String.class.equals(clazz)) {
                    return (T) string.content;
                }
                throw new JsonException(clazz + " is not a string type");
            case JsonObject object:
                return asProvidedObject(object, clazz);
            case JsonArray array:
                return asProvidedArray(array, clazz);
            default:
                throw new IllegalStateException("Unexpected value: " + source);
        }
    }

    //преобразует объект JsonArray в массив или коллекцию, указанную типом clazz
    //Используется в asProvided
    //+
    private <T> T asProvidedArray(JsonArray array, Class<T> clazz) {
        int size = array.elements.size();
        final var values = array.elements;
        // если класс -  массив
        if (clazz.isArray()) {
            //извлекается тип массива
            Class<?> componentType = clazz.getComponentType();
            // создается новый массив того же типа и размера
            var result = Array.newInstance(componentType, size);

            for (int i = 0; i < size; i++) {
            /*    Для каждого индекса i вызывается метод asProvided, который преобразует значение
                из списка values в нужный тип элемента массива и
                присваивает его соответствующему индексу массива result с помощью Array.set().*/
                Array.set(result, i, asProvided(values.get(i), componentType));
            }
            return (T) result;
        }
        if (!Collection.class.isAssignableFrom(clazz)) { // родитель
            throw new JsonException(clazz + " is not a collection");
        }
        try {
            T result;
            // еcли коллекция  - List
            if (clazz.isAssignableFrom(List.class)) {
                result = (T) new ArrayList();
            } else {
                // получение конструктора класса
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

    //+
    //Используется в asProvided
    private <T> T asProvidedObject(JsonObject object, Class<T> clazz) {
        try {
            T result;
            // является ли clazz подтипом Map
            if (Map.class.isAssignableFrom(clazz)) {
                result = (T) new LinkedHashMap();
                //1. ищет метод с названием puttAll и параметрами nипа Map
                //2. Вызывает на объекте result этот метод с параметром  - возвращаемым из  asOrdinaryMap(object)
                clazz.getMethod("putAll", Map.class).invoke(result, asOrdinaryMap(object));
                return result;
            }
            result = clazz.getConstructor().newInstance();
            for (var entry : object.entries.entrySet()) {
                final Field field = clazz.getDeclaredField(entry.getKey()); // для каждого ключа в поле объекта -  ищется соотвествующее поле
                field.setAccessible(true); // получаем доступ к приватным полям
                field.set(result, asProvided(entry.getValue(), field.getType())); // присваивается значение из Json объекта
            }
            return result;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    //
    private Object asOrdinary(JsonValue json) {
        switch (json) { //switch expression
            case JsonObject o:
                return asOrdinaryMap(o);
            // instanceOf
            case JsonArray array:
                return asOrdinaryList(array);
            case JsonLiteral literal:
                return literal;
            case JsonNumber number:
                return number.value;
            case JsonString string:
                return string.content;
            default:
                throw new IllegalStateException("Unexpected value: " + json);
        }
    }

    private List<Object> asOrdinaryList(JsonArray array) {
        List<Object> list = new ArrayList<>();
        for (JsonValue element : array.elements) {
            Object ordinary = asOrdinary(element);
            list.add(ordinary);
        }
        return list;
    }
    //используется в asProvidedObject для приведения JsonObject к Map
    private Map<String, Object> asOrdinaryMap(JsonObject o) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();//Создаётся новая карта map типа LinkedHashMap, которая будет сохранять порядок вставки.
        for (Map.Entry<String, JsonValue> e : o.entries.entrySet()) {
            //Метод merge пытается добавить в карту новую пару ключ-значение.
            //Если ключ уже существует - выбрасываем исключение
            map.merge(e.getKey(), asOrdinary(e.getValue()), (x, y) -> {
                throw new IllegalStateException();
            });
        }
        return map;
    }


}
