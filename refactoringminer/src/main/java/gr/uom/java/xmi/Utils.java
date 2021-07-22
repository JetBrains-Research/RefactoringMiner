package gr.uom.java.xmi;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

public class Utils {
    private Utils() {}

    /**
     * BiFunction for using with {@link java.util.Map#compute}. Add element to List or create new List with that element
     *
     * @param value Element to add in list
     */
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
