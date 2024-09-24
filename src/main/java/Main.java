import model.JsonArray;
import model.JsonObject;
import model.JsonValue;

import java.util.List;
import java.util.Map;

// Примеры использования библиотеки
public class Main {
    public static void main(String[] args) {
        // из строки в json
        System.out.println("Example1: ");
        JsonObject jsonObject1 = Json.parseJsonObject("""
            { "str": "str1", "array":  [1,2,3,"four"], "object" : {"name": "Alex","age":30} }
        """);
        System.out.println(jsonObject1);



        // из строки в Map объектов
        System.out.println("\nExample2: ");
        Map<String, Object> parsedObject1 = Json.parseObject("""
            { "str": "str1", "value": "123"  }
        """);
        for (Map.Entry<String, Object> entry : parsedObject1.entrySet()){
            System.out.println( entry.getKey() + ": "+ entry.getValue());
        }

        // из POJO в JsonObject
        System.out.println("\nExample3: ");
        class Person {
            String name;
            int age;
            int[] values;
            Person friend;
            public Person() {
            }
        }
        Person p1 = new Person();
        p1.name = "Alex";
        p1.age = 10;
        p1.values = new int[]{1,2,3,4};
        Person p2 = new Person();
        p2.name = "Ivan";
        p2.age = 20;
        p1.friend = p2;
        JsonValue jsonObject2 = Json.from(p1);

        System.out.println(jsonObject2);



        // из String в POJO
        System.out.println("\nExample4: ");
        Person p3 = Json.parseObject("""
                {"values": [1, 2, 3, 4], "name": "Alex", "friend": {"values": null, "name": "Ivan", "friend": null, "age": 20}, "age": 10}
                """,Person.class);
        JsonValue jsonObject3 = Json.from(p3);
        System.out.println(jsonObject3);


        // из String в List POJO
        System.out.println("\nExample5: ");

        List<Object> objectList= Json.parseArray("""
                [{"values0": null, "name0": "Alex0"},
                {"values": null, "name": "Alex", "friend": null, "age": 10}]
                """);
        JsonValue jsonObject4 = Json.from(objectList.get(0));
        System.out.println(jsonObject4);
        JsonValue jsonObject5 = Json.from(objectList.get(1));
        System.out.println(jsonObject5);



        // из String в JsonArray
        System.out.println("\nExample6: ");

        JsonArray jsonArray= Json.parseJsonArray("""
                [{"values": [1, 2, 3, 4], "name": "Alex", "friend": {"values": null, "name": "Ivan", "friend": null, "age": 20}, "age": 10},
                {"values": null, "name": "Alex", "friend": null, "age": 10}]
                """);
        List<JsonValue> ls = jsonArray.getListOfJsonValues();
        for(var i:ls){
            System.out.println(i);
        }

    }
}
