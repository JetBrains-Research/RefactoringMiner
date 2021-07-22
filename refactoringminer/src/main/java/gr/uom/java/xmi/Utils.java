package gr.uom.java.xmi;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

public class Utils {
    private Utils() {}

    public static <V> BiFunction<Object, List<V>, List<V>> createOrAppend(V value) {
        return (key, values) -> {
            if (values == null) {
                List<V> list = new ArrayList<>();
                list.add(value);
                return list;
            } else {
                values.add(value);
                return values;
            }
        };
    }
}
