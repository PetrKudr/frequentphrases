package answers;

/**
 *
 * @author petrk
 */
public class Palindrome {
    
    private static final String PALINDROME_1 = "accbccbcca";
    private static final String PALINDROME_2 = "accbcbcca";
    private static final String PALINDROME_3 = "a";
    private static final String PALINDROME_4 = "";
    private static final String NOT_PALINDROME_1 = "accbccdcca";
    private static final String NOT_PALINDROME_2 = "accbcdcca";
    
    public static void main(String args[]) {
        System.out.println("\"" + PALINDROME_1 +"\" is " + (isPalindrome(PALINDROME_1) ? "" : " not ") + "palindrome");
        System.out.println("\"" + PALINDROME_2 +"\" is " + (isPalindrome(PALINDROME_2) ? "" : " not ") + "palindrome");
        System.out.println("\"" + PALINDROME_3 +"\" is " + (isPalindrome(PALINDROME_3) ? "" : " not ") + "palindrome");
        System.out.println("\"" + PALINDROME_4 +"\" is " + (isPalindrome(PALINDROME_4) ? "" : " not ") + "palindrome");
        System.out.println("\"" + NOT_PALINDROME_1 +"\" is " + (isPalindrome(NOT_PALINDROME_1) ? "" : "not ") + "palindrome");
        System.out.println("\"" + NOT_PALINDROME_2 +"\" is " + (isPalindrome(NOT_PALINDROME_2) ? "" : "not ") + "palindrome");
    }
    
    // Time complexity - O(n)
    // Memory complexity - O(1)
    public static boolean isPalindrome(String str) {
        int left = 0;
        int right = str.length() - 1;
        while (left < right && str.charAt(left) == str.charAt(right)) {
            ++left;
            --right;
        }
        return right <= left;
    }
}
