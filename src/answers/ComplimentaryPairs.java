package answers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author petrk
 */
public class ComplimentaryPairs {
    
    private static final int ARRAY[] = {6, 7, 5, 2, 15, 65, 7, 14, 21, 1, -4, 0, 18};
    
    public static void main(String args[]) {
        int K = 100;
        System.out.print(K);
        System.out.print(" ");
        detect(ARRAY, K, (int i, int j) -> {
            System.out.print(
                "(" + i + ", " + j + ") "
            );
        });
    }
    
    // Time complexity - O(n) in average, O(n^2) in worst case
    // Memory complexity - O(n)
    public static void detect(int array[], int K, Consumer consumer) {
        Map<Integer, List<Integer>> mapping = new HashMap<Integer, List<Integer>>();
        for (int i = 0; i < array.length; ++i) {
            putInMapOfLists(mapping, array[i], i);
        }
        for (int i = 0; i < array.length; ++i) {
            int complimentary = K - array[i];
            List<Integer> indexes = mapping.get(complimentary);
            if (indexes != null) {
                for (Integer j : indexes) {
                    // consider (i, j) and (j, i) similar
                    if (j >= i) {
                        consumer.consume(i, j);
                    }
                }
            }
        }
    }
    
    private static <K, T> void putInMapOfLists(Map<K, List<T>> map, K key, T value) {
        List<T> list = map.get(key);
        if (list == null) {
            list = new ArrayList<>();
            map.put(key, list);
        }
        list.add(value);
    }
    
    @FunctionalInterface
    public static interface Consumer {
        void consume(int i, int j);
    }
}
