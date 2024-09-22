package model;

import java.math.BigDecimal;

public class JsonNumber implements JsonValue {
    public final Number value;

    public JsonNumber(int value) {
        this.value = value;
    }
    public JsonNumber(long value) {
        this.value = value;
    }
    public JsonNumber(double value) {
        this.value = value;
    }
    public JsonNumber(BigDecimal value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
