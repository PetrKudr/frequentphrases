package answers;

/**
 *
 * @author petrk
 */
public class Palindrome {
    
    private static final String PALINDROME_1 = "accbccbcca";
    private static final String PALINDROME_2 = "accbcbcca";
    private static final String NOT_PALINDROME_1 = "accbccdcca";
    
    public static void main(String args[]) {
        System.out.println("\"" + PALINDROME_1 +"\" is " + (isPalindrome(PALINDROME_1) ? "" : " not ") + "palindrome");
        System.out.println("\"" + PALINDROME_2 +"\" is " + (isPalindrome(PALINDROME_2) ? "" : " not ") + "palindrome");
        System.out.println("\"" + NOT_PALINDROME_1 +"\" is " + (isPalindrome(NOT_PALINDROME_1) ? "" : "not ") + "palindrome");
    }
    
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
