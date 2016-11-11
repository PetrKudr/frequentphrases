package frequentelements;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 *
 * @author toor
 */
public class TestGenerator {
    
    private static final String PATH = "${HOME}/assignments/test02";
    
    private static final int PHRASES_NUMBER = 100000;
    
    public static void main(String args[]) throws IOException {
        Dictionary dictionary = readDictionary();
        dictionary.setMaxWords(3);
        writeTest(dictionary);
    }

    private static Dictionary readDictionary() throws IOException {
        InputStream fIn = TestGenerator.class.getResourceAsStream("dictionary");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(fIn))) {
            List<String> lines = reader.lines().collect(Collectors.toCollection(() -> new ArrayList()));
            return new Dictionary(lines);
        }
    }
    
    
    private static void writeTest(Dictionary dictionary) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(TopPhrases.fixPath(PATH)))) {
            for (int i = 0; i < PHRASES_NUMBER; i += 50) {
                writer.write(dictionary.makeRandomPhrase());
                for (int k = 1; k < 50; k++) {
                    writer.write(" | ");
                    writer.write(dictionary.makeRandomPhrase());
                }
                writer.write('\n');
            }
        }
    }
    
    private static class Dictionary {
        
        private final Random randomGenerator = new Random();
        
        private final List<String> words;
        
        private int maxWords = 2;
        
        private int minWords= 1;

        public Dictionary(List<String> words) {
            this.words = words;
        }

        public void setMaxWords(int maxWords) {
            this.maxWords = maxWords;
        }

        public void setMinWords(int minWords) {
            this.minWords = minWords;
        }
        
        public String makeRandomPhrase() {
            int numOfWords = randomGenerator.nextInt(maxWords - minWords + 1) + 1;
            if (numOfWords > 1) {
                StringBuilder sb = new StringBuilder(randomGenerator.nextInt(words.size()));
                while (numOfWords > 1) {
                    sb.append(' ');
                    sb.append(words.get(randomGenerator.nextInt(words.size())));
                    --numOfWords;
                }
                return sb.toString();
            } else {
                return words.get(randomGenerator.nextInt(words.size()));
            }
        }
    }
}
