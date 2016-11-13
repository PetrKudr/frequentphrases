package answers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 *
 * @author petrk
 */
public class TopPhrases {
    
//    private final static int MERGE_SIMULTANEOUSLY = 2;
//    
//    private final static int BEST_CHUNK_SIZE = 32; // bytes
    
    private final static int MERGE_SIMULTANEOUSLY = 10;
    
    private final static int BEST_CHUNK_SIZE = 1024 * 10; // bytes
    
    private final static int HOW_MANY_TOP_PHRASES = 10;

    public static void main(String[] args) throws Exception {
        TopPhrases detector = new TopPhrases();
        Collection<String> topPhrases = detector.detect(new File(fixPath("${HOME}/assignments/test02")));
        for (String phrase : topPhrases) {
            System.out.println(phrase);
        }
    }
    
    // Time complexity - O(n * log(n))
    // Memory (disc) complexity - O(n)
    public Collection<String> detect(File path) throws IOException {
        TemporaryStorage firstStorage = null;
        TemporaryStorage secondStorage = null;
        try {
            firstStorage = new FileBasedTemporaryStorage();
            secondStorage = new FileBasedTemporaryStorage();
            Pair<TemporaryStorage, Integer> sorted = externalSort(firstStorage, secondStorage, path);
            return extractTopPhrases(sorted.first, sorted.second, HOW_MANY_TOP_PHRASES);
        } finally {
            if (firstStorage != null) {
                firstStorage.clearStorage();
            }
            if (secondStorage != null) {
                secondStorage.clearStorage();
            }
        }
    }
    
    private Collection<String> extractTopPhrases(TemporaryStorage storage, int file, int howMany) throws IOException {
        final TreeMap<Long, List<String>> result = new TreeMap<>((Long o1, Long o2) -> o2.compareTo(o1)); // descending order
        int size = 0;
        storage.open(file, true, false);
        Phrase phrase;
        while ((phrase = storage.read(file)) != null) {
            putInMapOfLists(result, phrase.counter, phrase.text);
            if (size == howMany) {
                Map.Entry<Long, List<String>> lastEntry = result.lastEntry();
                List<String> value = lastEntry.getValue();
                value.remove(0); // remove any phrase (let it be the first one)
                if (value.isEmpty()) {
                    result.remove(lastEntry.getKey());
                }
            } else {
                ++size;
            }
        }
        storage.close(file);
        return result.values().stream()
                .flatMap((list) -> list.stream())
                .collect(Collectors.toList());
    }
    
    private int getBestChunkSize(File input) {
        return BEST_CHUNK_SIZE; 
    }

    private Pair<TemporaryStorage, Integer> externalSort(TemporaryStorage firstStorage, TemporaryStorage secondStorage, File input) throws IOException {
        int maxChunkSize = getBestChunkSize(input);
        List<Integer> chunkFiles = new ArrayList<>();
        try (PhrasesReader reader = new PhrasesReader(new BufferedReader(new FileReader(input)))) {
            while (reader.hasNext()) {
                Collection<Phrase> chunk = readAndSortChunk(reader, maxChunkSize);
                chunkFiles.add(writeChunk(firstStorage, chunk));
            }
        }
        Pair<TemporaryStorage, Integer> sorted = externalSortBatch(firstStorage, secondStorage, chunkFiles);
        assert(DIAGS.dump(sorted));
        return sorted;
    }
    
    private Pair<TemporaryStorage, Integer> externalSortBatch(TemporaryStorage readStorage, TemporaryStorage writeStorage, List<Integer> inputFiles) throws IOException {
        if (inputFiles.size() > 1) {
            List<Integer> outputFiles = new ArrayList();
            int from = 0;
            int to = (inputFiles.size() > readStorage.getSimultaneousAccessNumber()) 
                    ? readStorage.getSimultaneousAccessNumber() 
                    : inputFiles.size();
            while (from < to) {
                outputFiles.add(mergeFiles(readStorage, writeStorage, inputFiles.subList(from, to)));
                from = to;
                to = Math.min(to + readStorage.getSimultaneousAccessNumber(), inputFiles.size());
            }
            readStorage.clearStorage();
            assert(DIAGS.addPhase(inputFiles.size(), outputFiles.size()));
            return externalSortBatch(writeStorage, readStorage, outputFiles);
        }
        assert inputFiles.size() == 1;
        return Pair.of(readStorage, inputFiles.get(0));
    }
    
    private int mergeFiles(TemporaryStorage readStorage, TemporaryStorage writeStorage, List<Integer> input) throws IOException {
        List<Pair<Integer, Phrase>> readers = new ArrayList<>();
        int output = -1;
        try {
            output = writeStorage.create();
            writeStorage.open(output, false, true);
            for (Integer id : input) {
                readStorage.open(id, true, false);
                readers.add(Pair.of(
                        id, 
                        (Phrase) null
                ));
            }
            Phrase current = fetchFirstLine(readStorage, readers);
            while (current != null) {
                Phrase next = fetchNextLine(readStorage, readers);
                if (next != null) {
                    if (current.compareByText(next) != 0) {
                        assert current.compareByText(next) < 0;
                        writeStorage.write(output, current);
                        current = next;
                    } else {
                        current.incCounter(next.getCounter());
                    }
                } else {
                    writeStorage.write(output, current);
                    current = null;
                }
            }
        } finally {
            readStorage.closeAll();
            writeStorage.close(output);
        }
        return output;
    }
    
    private Phrase fetchFirstLine(TemporaryStorage storage, List<Pair<Integer, Phrase>> readers) throws IOException {
        for (Pair<Integer, Phrase> reader : readers) {
            reader.second = storage.read(reader.first);
            assert reader.second != null : "Why the first line in chunk file is empty?";
        }
        return fetchNextLine(storage, readers);
    }
    
    // TODO: sort readers by their phrases. This will reduce complexity for 
    // cases when many files processed simultaneously
    private Phrase fetchNextLine(TemporaryStorage storage, List<Pair<Integer, Phrase>> readers) throws IOException {
        if (readers.isEmpty()) {
            return null;
        }
        Pair<Integer, Phrase> bestReader = null;
        for (Pair<Integer, Phrase> reader : readers) {
            assert reader.second != null : "Why prepared reader has empty line?";
            if (bestReader == null) {
                bestReader = reader;
            } else {
                int comparison = bestReader.second.compareByText(reader.second);
                if (comparison > 0) {
                    bestReader = reader;
                }
            }
        }
        Phrase retval = bestReader.second;
        bestReader.second = storage.read(bestReader.first);
        if (bestReader.second == null) {
            readers.remove(bestReader);
        }
        return retval;
    }
    
    private Collection<Phrase> readAndSortChunk(PhrasesReader reader, int approxMaxChunkSize) throws IOException {
        TreeMap<String, Phrase> chunk = new TreeMap<>();
        int approxTotalSize = 0;
        String text;
        while (approxTotalSize < approxMaxChunkSize && (text = reader.next()) != null) {
            Phrase existing = chunk.get(text);
            if (existing != null) {
                existing.incCounter();
            } else {
                approxTotalSize += text.length() * Character.BYTES;
                chunk.put(text, new Phrase(text, 1));
            }
        }
        return chunk.values();
    }
   
    private int writeChunk(TemporaryStorage storage, Collection<Phrase> chunk) throws IOException {
        int outputId = storage.create();
        storage.open(outputId, false, true);
        for (Phrase phase : chunk) {
            storage.write(outputId, phase);
        }
        storage.close(outputId);
        return outputId;
    }
    
    private static <K, T> void putInMapOfLists(Map<K, List<T>> map, K key, T value) {
        List<T> list = map.get(key);
        if (list == null) {
            list = new ArrayList<>();
            map.put(key, list);
        }
        list.add(value);
    }
    
    public static String fixPath(String path) {
        String home = System.getenv().get("HOME");
        if (home != null && !home.isEmpty()) {
            return path.replace("${HOME}", home);
        }
        return path;
    }
    
    public static interface TemporaryStorage {
        
        int create() throws IOException;
        
        boolean open(int id, boolean read, boolean write) throws IOException;
        
        void close(int id) throws IOException;
        
        void closeAll() throws IOException;
        
        Phrase read(int id) throws IOException;
        
        int write(int id, Phrase value) throws IOException;
        
        // Number of files opened simultaneously without performance issues
        int getSimultaneousAccessNumber();
        
        void clearStorage() throws IOException;
    }
    
    // TODO: implement storage via Memory mapped files to measure performance
    private static class MemoryMappedFileBasedTemporaryStorage implements TemporaryStorage {
        
        private final long inputFileSize = 0;
        
        private long mappedPosition = 0;
        
        private int mappedSize = 0;
        
        private int position = 0;

        private int lastId = 0;
        
        private MappedByteBuffer currentMappedBuffer;

        public MemoryMappedFileBasedTemporaryStorage(long inputFileSize) throws IOException {
            throw new UnsupportedOperationException("Not supported yet."); 
        }
        
        private MappedByteBuffer getBuffer(int position) {
            throw new UnsupportedOperationException("Not supported yet."); 
        }

        @Override
        public int create() throws IOException {
            throw new UnsupportedOperationException("Not supported yet."); 
        }

        @Override
        public boolean open(int id, boolean read, boolean write) throws IOException {
            throw new UnsupportedOperationException("Not supported yet."); 
        }

        @Override
        public void close(int id) throws IOException {
            throw new UnsupportedOperationException("Not supported yet."); 
        }

        @Override
        public void closeAll() throws IOException {
            throw new UnsupportedOperationException("Not supported yet."); 
        }

        @Override
        public void clearStorage() throws IOException {
            throw new UnsupportedOperationException("Not supported yet."); 
        }

        @Override
        public Phrase read(int id) throws IOException {
            throw new UnsupportedOperationException("Not supported yet."); 
        }

        @Override
        public int write(int id, Phrase value) throws IOException {
            throw new UnsupportedOperationException("Not supported yet."); 
        }

        @Override
        public int getSimultaneousAccessNumber() {
            return 8;
        }
    }
    
    private static class FileBasedTemporaryStorage implements TemporaryStorage {

        private final List<Triple<Path, BufferedReader, BufferedWriter>> files = new ArrayList();
        
        private int lastId = 0;
        
        @Override
        public int create() throws IOException {
            files.add(Triple.of(Files.createTempFile("sortTemporary", ".dat"), null, null));
            return lastId++;
        }

        @Override
        public boolean open(int id, boolean read, boolean write) throws IOException {
            if (read && write) {
                throw new UnsupportedOperationException("Not supported yet!");
            }
            Triple<Path, BufferedReader, BufferedWriter> stored = files.get(id);
            assert stored != null : "Why use unknown temporary?";
            if (read && stored.second == null) {
                stored.second = new BufferedReader(new FileReader(stored.first.toFile()));
            } else if (write && stored.second == null) {
                stored.third = new BufferedWriter(new FileWriter(stored.first.toFile()));
            }
            return true;
        }

        @Override
        public void close(int id) throws IOException {
            Triple<Path, BufferedReader, BufferedWriter> stored = files.get(id);
            assert stored != null : "Why use unknown temporary?";
            if (stored.second != null) {
                stored.second.close();
                stored.second = null;
            }
            if (stored.third != null) {
                stored.third.close();
                stored.third = null;
            }
        }

        @Override
        public void closeAll() throws IOException {
            for (int id = 0; id < lastId; ++id) {
                close(id);
            }
        }

        @Override
        public void clearStorage() throws IOException {
            for (int id = 0; id < lastId; ++id) {
                close(id);
                files.get(id).first.toFile().delete();
            }
        }

        @Override
        public Phrase read(int id) throws IOException {
            Triple<Path, BufferedReader, BufferedWriter> stored = files.get(id);
            assert stored != null;
            String line = stored.second.readLine();
            if (line != null) {
                int counter = Integer.valueOf(line.substring(0, line.indexOf(':')));
                return new Phrase(line.substring(line.indexOf(':') + 1), counter);
            }
            return null;
        }

        @Override
        public int write(int id, Phrase value) throws IOException {
            Triple<Path, BufferedReader, BufferedWriter> stored = files.get(id);
            assert stored != null;
            String strValue = String.valueOf(value.getCounter()) + ":" + value.getText();
            stored.third.write(strValue);
            stored.third.write('\n');
            return strValue.length() * Character.BYTES;
        }

        @Override
        public int getSimultaneousAccessNumber() {
            return MERGE_SIMULTANEOUSLY;
        }
    }
    
    private static class PhrasesReader implements AutoCloseable {
        
        private final BufferedReader reader;
        
        private StringTokenizer tokenizer;
        
        private String nextPhrase;
        
        public PhrasesReader(BufferedReader reader) throws IOException {
            this.reader = reader;
            this.tokenizer = null;
            try {
                nextPhrase = computeNext();
            } catch (IOException ex) {
                reader.close();
            }
        }
        
        public boolean hasNext() {
            return nextPhrase != null;
        }
        
        public String next() throws IOException {
            String retval = nextPhrase;
            nextPhrase = computeNext();
            return retval;
        }
        
        private String computeNext() throws IOException {
            if (tokenizer != null && tokenizer.hasMoreTokens()) {
                return tokenizer.nextToken().trim();
            }
            String line = reader.readLine();
            if (line != null) {
                tokenizer = new StringTokenizer(line, "|");
                if (tokenizer.hasMoreTokens()) {
                    return tokenizer.nextToken().trim();
                }
            }
            return null;
        }

        @Override
        public void close() throws IOException {
            reader.close();
        }
    }
    
    public static class Phrase {
        
        public final String text;
        
        private long counter;

        public Phrase(String text, int counter) {
            this.text = text;
            this.counter = counter;
        }
        
        public String getText() {
            return text;
        }
        
        public long getCounter() {
            return counter;
        }
        
        public void incCounter() {
            ++counter;
        }
        
        public void incCounter(long amount) {
            counter += amount;
        }
        
        public int compareByCounter(Phrase o) {
            if (counter < o.counter) {
                return -1;
            } else if (counter == o.counter) {
                return 0;
            }
            return 1;
        }

        public int compareByText(Phrase o) {
            return text.compareTo(o.text);
        }

        @Override
        public String toString() {
            return "[" + counter + ", " + text + "]";
        }
    }
    
    private static class Pair<F, S> {
        
        public F first;
        
        public S second;
        
        public static <F, S> Pair<F, S> of(F first, S second) {
            return new Pair(first, second);
        }

        private Pair(F first, S second) {
            this.first = first;
            this.second = second;
        }        
    }
    
    private static class Triple<F, S, T> {
        
        public F first;
        
        public S second;
        
        public T third;
        
        public static <F, S, T> Triple<F, S, T> of(F first, S second, T third) {
            return new Triple(first, second, third);
        }

        private Triple(F first, S second, T third) {
            this.first = first;
            this.second = second;
            this.third = third;
        }        
    }
    
    ////////////////////////////////////////////////
    // For debug purposes
    
    private static final Diagnostics DIAGS = new Diagnostics();
    
    static class Diagnostics {
        
        private final List<Phase> phases = new ArrayList<>();
        
        public boolean addPhase(int readFiles, int writtenFiles) {
            phases.add(new Phase(readFiles, writtenFiles));
            return true;
        }
        
        public boolean dump(Pair<TemporaryStorage, Integer> sorted) {
            try {
                System.out.println("//////////////////////////////////////////");
                System.out.println("// DUMP OF SORTING STATISTICS");
                TemporaryStorage storage = sorted.first;
                int file = sorted.second;
                storage.open(file, true, false);
                Phrase phrase;
                while ((phrase = storage.read(file)) != null) {
                    System.out.println(phrase);
                }
                storage.close(file);
                
                System.out.println();
                System.out.println();
                for (int i = 0; i < phases.size(); ++i) {
                    Phase phase = phases.get(i);
                    System.out.println("Phase " + i + ": " 
                            + phase.readFiles + " read => " 
                            + phase.writtenFiles + " written");
                }
                System.out.println();
                System.out.println("//////////////////////////////////////////");
                System.out.println();
            } catch (IOException ex) {
                // do nothing
            }
            return true;
        }
        
        private static class Phase {
            public int readFiles;
            public int writtenFiles;
            public Phase(int readFiles, int writtenFiles) {
                this.readFiles = readFiles;
                this.writtenFiles = writtenFiles;
            }
        }
    }
}
