package answers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author petrk
 */
public class ComplimentaryPairs {
    
    private static final int ARRAY[] = {6, 7, 5, 2, 15, 65, 7, 14, 21, 1, -4, 0, 18};
    
    private static final int K = 14;
    
    public static void main(String args[]) {
        detect(ARRAY, K, (int i, int j) -> {
            System.out.println(
                "(" + i + ", " + j + ") = " 
                + ARRAY[i] + " + " + ARRAY[j] + " = "
                + (ARRAY[i] + ARRAY[j])
            );
        });
    }
    
    public static void detect(int array[], int K, Consumer consumer) {
        if (array.length > 0) {
            // array with original positions. Stored index doesn't affect on 
            // order after sorting because it is stored in lower bits
            long workArray[] = new long[array.length];
            for (int i = 0; i < array.length; ++i) {
                workArray[i] = wrapIntInt(array[i], i);
            }
            Arrays.sort(workArray);
            int left = 0;
            int right = workArray.length - 1;
            int lVal = firstInt(workArray[left]);
            int rVal = firstInt(workArray[right]);
            while (left <= right) {
                assert lVal <= rVal;
                int sum = lVal + rVal;
                if (sum < K) {
                    lVal = firstInt(workArray[++left]);
                } else if (sum > K) {
                    rVal = firstInt(workArray[--right]);
                } else if (sum == K) {
                    if (lVal != rVal) {
                        int rr = right;
                        while (rVal == workArray[--right]);
                        rVal = firstInt(workArray[right]);
                        // (right, rr] - subarray with numbers equal to rVal
                        
                        int ll = left;
                        while (lVal == workArray[++left]);
                        lVal = firstInt(workArray[left]);
                        // [ll, left) - subarray with numbers equal to lVal
                        
                        for (int l = ll; l < left; ++l) {
                            for (int r = rr; r > right; --r)  {
                                consumer.consume(secondInt(workArray[l]), secondInt(workArray[r]));
                            }
                        }
                    } else {
                        for (int l = left; l <= right; ++l) {
                            for (int r = right; r >= left; --r)  {
                                consumer.consume(secondInt(workArray[l]), secondInt(workArray[r]));
                            }
                        }
                        break;
                    }
                }
            }
        }
    }
    
    @FunctionalInterface
    public static interface Consumer {
        void consume(int i, int j);
    }
    
    private static long wrapIntInt(int First, int Second) {
        long Out = (((long) (First)) << 32) | Integer.toUnsignedLong(Second);
        assert firstInt(Out) == First;
        assert secondInt(Out) == Second;
        return Out;
    }
    
    private static int firstInt(long wrapped) {
      return (int)(wrapped >>> 32);
    }

    private static int secondInt(long wrapped) {
      assert (int)(wrapped & 0xFFFFFFFFL) == (int)wrapped;
      return (int)(wrapped);
    }
}
