/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package frequentelements;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 *
 * @author toor
 */
public class FrequentElementsDetector {
    
    private final static int MERGE_SIMULTANEOUSLY = 2;

    public static void main(String[] args) throws Exception {
        FrequentElementsDetector detector = new FrequentElementsDetector();
        detector.detect(new File("/home/petrk/assignments/test01"));
    }
    
    public List<String> detect(File path) throws IOException {
        TemporaryStorage firstStorage = null;
        TemporaryStorage secondStorage = null;
        try {
            firstStorage = new FileBasedTemporaryStorage();
            secondStorage = new FileBasedTemporaryStorage();
            externalSort(firstStorage, secondStorage, path);
        } finally {
            if (firstStorage != null) {
                firstStorage.clear();
            }
            if (secondStorage != null) {
                secondStorage.clear();
            }
        }
        return Collections.emptyList();
    }

    private void externalSort(TemporaryStorage firstStorage, TemporaryStorage secondStorage, File input) throws IOException {
        int maxChunkSize = getBestChunkSize(input);
        List<Integer> chunkFiles = new ArrayList<>();
        try (BufferedReader fIn = new BufferedReader(new FileReader(input))) {
            PhrasesReader reader = new PhrasesReader(fIn);
            Collection<Phrase> chunk = readAndSortChunk(reader, maxChunkSize);
            chunkFiles.add(writeChunk(firstStorage, chunk));
        }
        Pair<TemporaryStorage, Integer> sorted = externalSortBatch(firstStorage, secondStorage, chunkFiles);
        TemporaryStorage storage = sorted.first;
        int file = sorted.second;
        storage.open(file, true, false);
        Phrase phrase;
        while ((phrase = storage.read(file)) != null) {
            System.out.println(phrase);
        }
    }
    
    private Pair<TemporaryStorage, Integer> externalSortBatch(TemporaryStorage readStorage, TemporaryStorage writeStorage, List<Integer> files) throws IOException {
        if (files.size() > 1) {
            List<Integer> merged = new ArrayList();
            int from = 0;
            int to = (files.size() > readStorage.getSimultaneousAccessNumber()) 
                    ? readStorage.getSimultaneousAccessNumber() 
                    : files.size();
            while (from < to) {
                merged.add(mergeChunkFiles(readStorage, writeStorage, files.subList(from, to)));
                from = to;
                to = Math.min(to + readStorage.getSimultaneousAccessNumber(), files.size());
            }
            readStorage.clear();
            return externalSortBatch(writeStorage, readStorage, merged);
        }
        assert files.size() == 1;
        return Pair.of(readStorage, files.get(0));
    }
    
    private int mergeChunkFiles(TemporaryStorage readStorage, TemporaryStorage writeStorage, List<Integer> input) throws IOException {
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
                    if (current.compareTo(next) != 0) {
                        assert current.compareTo(next) < 0;
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
    
    private Phrase fetchNextLine(TemporaryStorage storage, List<Pair<Integer, Phrase>> readers) throws IOException {
        Pair<Integer, Phrase> best = null;
        for (Pair<Integer, Phrase> reader : readers) {
            assert reader.second != null : "Why prepared reader has empty line?";
            if (best == null) {
                best = reader;
            } else {
                int comparison = best.second.compareTo(reader.second);
                if (comparison > 0) {
                    best = reader;
                }
            }
        }
        Phrase retval = best.second;
        best.second = storage.read(best.first);
        if (best.second == null) {
            readers.remove(best);
        }
        return retval;
    }
    
    private int getBestChunkSize(File input) {
        return 10 * 1024 * 1024; // 10 MB
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
    
    public static interface TemporaryStorage {
        
        int create() throws IOException;
        
        boolean open(int id, boolean read, boolean write) throws IOException;
        
        void close(int id) throws IOException;
        
        void closeAll() throws IOException;
        
        void clear() throws IOException;
        
        Phrase read(int id) throws IOException;
        
        int write(int id, Phrase value) throws IOException;
        
        // Number of files opened simultaneously without performance issues
        int getSimultaneousAccessNumber();
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
        public void clear() throws IOException {
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
            String strValue = value.asString();
            stored.third.write(strValue);
            stored.third.write('\n');
            return strValue.length() * Character.BYTES;
        }

        @Override
        public int getSimultaneousAccessNumber() {
            return MERGE_SIMULTANEOUSLY;
        }
    }
    
    private static class PhrasesReader {
        
        private final BufferedReader reader;
        
        private StringTokenizer tokenizer = null;
        
        private boolean finished = false;
        
        public PhrasesReader(BufferedReader reader) {
            this.reader = reader;
        }
        
        public String next() throws IOException {
            if (!finished) {
                if (tokenizer != null && tokenizer.hasMoreTokens()) {
                    return tokenizer.nextToken();
                }
                String line = reader.readLine();
                if (line != null) {
                    tokenizer = new StringTokenizer(line, "|");
                    if (tokenizer.hasMoreTokens()) {
                        return tokenizer.nextToken();
                    }
                }
                finished = true;
            }
            return null;
        }
    }
    
    public static class Phrase implements Comparable<Phrase>{
        
        public final String text;
        
        private int counter;

        public Phrase(String text, int counter) {
            this.text = text;
            this.counter = counter;
        }
        
        public CharSequence getText() {
            return text;
        }
        
        public int getCounter() {
            return counter;
        }
        
        public void incCounter() {
            ++counter;
        }
        
        public void incCounter(int amount) {
            counter += amount;
        }

        private String asString() {
            return String.valueOf(counter) + ":" + getText();
        }

        @Override
        public int compareTo(Phrase o) {
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
}
