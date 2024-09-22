import model.JsonObject;

import java.util.Map;

// Примеры использования библиотеки
public class Main {
    public static void main(String[] args) {
        JsonObject example = Json.parseJsonObject("""
            { "x": [12, 1.0e-3, {}], "": {"t": null} }
        """);
        System.out.println(example);
        Map<String, Object> parsed = Json.parseObject("""
                    { "x": [12, 1.0e-3, {}], "": {"t": null} }
                """);
        System.out.println(parsed);

        class Hero {
            int armor;
            double[] weights;
            Hero friend;

            public Hero() {
            }
        }
        Hero h1 = new Hero();
        h1.armor = -1;
        h1.weights = new double[] {100, 0.5};

        Hero h2 = new Hero();
        h2.armor = 2;
        h2.weights = new double[] {0.5, 12, 0};
        h2.friend = h1;
        System.out.println(Json.from(h2));

        Hero hero3 = Json.parseObject("""
                {"armor": 2, "friend": {"armor": -1, "friend": null, "weights": [100.0, 0.5]}, "weights": [0.5, 12.0, 0.0]}
                                """, Hero.class);
        hero3.armor = 22;
        System.out.println(Json.from(hero3));
    }
}
