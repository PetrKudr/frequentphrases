package answers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 *
 * @author petrk
 */
public class TopPhrasesTestGenerator {
    
    private static final String PATH = "${HOME}/assignments/test02";
    
    private static final boolean GENERATE_GOLDEN = true;
    
    private static final int PHRASES_NUMBER = 100000;
    
    public static void main(String args[]) throws IOException {
        Dictionary dictionary = readDictionary();
        dictionary.setMaxWords(3);
        writeTest(dictionary);
    }

    private static Dictionary readDictionary() throws IOException {
        InputStream fIn = TopPhrasesTestGenerator.class.getResourceAsStream("dictionary");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(fIn))) {
            List<String> lines = reader.lines().collect(Collectors.toCollection(() -> new ArrayList()));
            return new Dictionary(lines);
        }
    }
    
    private static void writeTest(Dictionary dictionary) throws IOException {
        Map<String, Integer> mapping = GENERATE_GOLDEN ? new HashMap<>() : null;
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(TopPhrases.fixPath(PATH)))) {
            for (int i = 0; i < PHRASES_NUMBER; i += 50) {
                writer.write(storePhrase(dictionary.makeRandomPhrase(), mapping));
                for (int k = 1; k < 50; k++) {
                    writer.write(" | ");
                    writer.write(storePhrase(dictionary.makeRandomPhrase(), mapping));
                }
                writer.write('\n');
            }
        }
        if (mapping != null) {
            List<Map.Entry<String, Integer>> values = new LinkedList(mapping.entrySet());
            Collections.sort(values, (e1, e2) -> e2.getValue().compareTo(e1.getValue()));
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(TopPhrases.fixPath(PATH) + ".golden"))) {
                for (Map.Entry<String, Integer> value : values) {
                    writer.write(String.valueOf(value.getValue()));
                    writer.write(": ");
                    writer.write(value.getKey());
                    writer.write("\n");
                }
            }
        }
    }
    
    private static String storePhrase(String phrase, Map<String, Integer> mapping) {
        if (mapping != null) {
            Integer counter = mapping.get(phrase);
            if (counter != null) {
                mapping.put(phrase, counter + 1);
            } else {
                mapping.put(phrase, 1);
            }
        }
        return phrase;
    }
    
    private static class Dictionary {
        
        private final Random randomGenerator = new Random();
        
        private final List<String> words;
        
        private int maxWords = 2;
        
        private int minWords= 1;

        public Dictionary(List<String> words) {
            this.words = new ArrayList<>(words.size());
            for (String word : words) {
                String trimmed = word.trim();
                if (trimmed.length() > 0) {
                    this.words.add(trimmed);
                }
            }
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
                StringBuilder sb = new StringBuilder(words.get(randomGenerator.nextInt(words.size())));
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
