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
    
/*
    // we divide the file into small blocks. If the blocks
    // are too small, we shall create too many temporary files. 
    // If they are too big, we shall be using too much memory. 
    public static long estimateBestSizeOfBlocks(File filetobesorted) {
        long sizeoffile = filetobesorted.length();
        // we don't want to open up much more than 1024 temporary files, better run
        // out of memory first. (Even 1024 is stretching it.)
        final int MAXTEMPFILES = 1024;
        long blocksize = sizeoffile / MAXTEMPFILES ;
        // on the other hand, we don't want to create many temporary files
        // for naught. If blocksize is smaller than half the free memory, grow it.
        long freemem = Runtime.getRuntime().freeMemory();
        if( blocksize < freemem/2)
            blocksize = freemem/2;
        else {
            if(blocksize >= freemem) 
              System.err.println("We expect to run out of memory. ");
        }
        return blocksize;
    }
 
     // This will simply load the file by blocks of x rows, then
     // sort them in-memory, and write the result to a bunch of 
     // temporary files that have to be merged later.
     // 
     // @param file some flat  file
     // @return a list of temporary flat files
 
    public static List<File> sortInBatch(File file, Comparator<String> cmp) throws IOException {
        List<File> files = new ArrayList<File>();
        BufferedReader fbr = new BufferedReader(new FileReader(file));
        long blocksize = estimateBestSizeOfBlocks(file);// in bytes
        try{
            List<String> tmplist =  new ArrayList<String>();
            String line = "";
            try {
                while(line != null) {
                    long currentblocksize = 0;// in bytes
                    while((currentblocksize < blocksize) 
                    &&(   (line = fbr.readLine()) != null) ){ // as long as you have 2MB
                        tmplist.add(line);
                        currentblocksize += line.length() // 2 + 40; // java uses 16 bits per character + 40 bytes of overhead (estimated)
                    }
                    files.add(sortAndSave(tmplist,cmp));
                    tmplist.clear();
                }
            } catch(EOFException oef) {
                if(tmplist.size()>0) {
                    files.add(sortAndSave(tmplist,cmp));
                    tmplist.clear();
                }
            }
        } finally {
            fbr.close();
        }
        return files;
    }
 
 
    public static File sortAndSave(List<String> tmplist, Comparator<String> cmp) throws IOException  {
        Collections.sort(tmplist,cmp);  // 
        File newtmpfile = File.createTempFile("sortInBatch", "flatfile");
        newtmpfile.deleteOnExit();
        BufferedWriter fbw = new BufferedWriter(new FileWriter(newtmpfile));
        try {
            for(String r : tmplist) {
                fbw.write(r);
                fbw.newLine();
            }
        } finally {
            fbw.close();
        }
        return newtmpfile;
    }
 
     // This merges a bunch of temporary flat files 
     // @param files
     // @param output file
     // @return The number of lines sorted. (P. Beaudoin)
 
    public static int mergeSortedFiles(List<File> files, File outputfile, final Comparator<String> cmp) throws IOException {
        PriorityQueue<BinaryFileBuffer> pq = new PriorityQueue<BinaryFileBuffer>(11, 
            new Comparator<BinaryFileBuffer>() {
              public int compare(BinaryFileBuffer i, BinaryFileBuffer j) {
                return cmp.compare(i.peek(), j.peek());
              }
            }
        );
        for (File f : files) {
            BinaryFileBuffer bfb = new BinaryFileBuffer(f);
            pq.add(bfb);
        }
        BufferedWriter fbw = new BufferedWriter(new FileWriter(outputfile));
        int rowcounter = 0;
        try {
            while(pq.size()>0) {
                BinaryFileBuffer bfb = pq.poll();
                String r = bfb.pop();
                fbw.write(r);
                fbw.newLine();
                ++rowcounter;
                if(bfb.empty()) {
                    bfb.fbr.close();
                    bfb.originalfile.delete();// we don't need you anymore
                } else {
                    pq.add(bfb); // add it back
                }
            }
        } finally { 
            fbw.close();
            for(BinaryFileBuffer bfb : pq ) bfb.close();
        }
        return rowcounter;
    }
 
    public static void main(String[] args) throws IOException {
        if(args.length<2) {
            System.out.println("please provide input and output file names");
            return;
        }
        String inputfile = args[0];
        String outputfile = args[1];
        Comparator<String> comparator = new Comparator<String>() {
            public int compare(String r1, String r2){
                return r1.compareTo(r2);}};
        List<File> l = sortInBatch(new File(inputfile), comparator) ;
        mergeSortedFiles(l, new File(outputfile), comparator);
    }
}
 
 
class BinaryFileBuffer  {
    public static int BUFFERSIZE = 2048;
    public BufferedReader fbr;
    public File originalfile;
    private String cache;
    private boolean empty;
     
    public BinaryFileBuffer(File f) throws IOException {
        originalfile = f;
        fbr = new BufferedReader(new FileReader(f), BUFFERSIZE);
        reload();
    }
     
    public boolean empty() {
        return empty;
    }
     
    private void reload() throws IOException {
        try {
          if((this.cache = fbr.readLine()) == null){
            empty = true;
            cache = null;
          }
          else{
            empty = false;
          }
      } catch(EOFException oef) {
        empty = true;
        cache = null;
      }
    }
     
    public void close() throws IOException {
        fbr.close();
    }
     
     
    public String peek() {
        if(empty()) return null;
        return cache.toString();
    }
    public String pop() throws IOException {
      String answer = peek();
        reload();
      return answer;
    }
     
     
 
}
*/
    private final static int MERGE_SIMULTANEOUSLY = 2;

    public static void main(String[] args) throws Exception {
        FrequentElementsDetector detector = new FrequentElementsDetector();
        List<String> detect = detector.detect(new File("/home/toor/assignments/test01"));
    }
    
    public List<String> detect(File path) throws IOException {
        TemporaryStorage firstStorage = new FileBasedTemporaryStorage();
        TemporaryStorage secondStorage = new FileBasedTemporaryStorage();
        externalSort(firstStorage, secondStorage, path);
        return Collections.emptyList();
    }

    private void externalSort(TemporaryStorage firstStorage, TemporaryStorage secondStorage, File input) throws IOException {
        int maxChunkSize = getBestChunkSize(input);
        List<Integer> chunkFiles = new ArrayList<>();
        try (BufferedReader fIn = new BufferedReader(new FileReader(input))) {
            PhrasesReader reader = new PhrasesReader(fIn);
            Collection<Phrase> chunk = readAndSortChunk(reader, maxChunkSize);
            int chunkFileId = firstStorage.create();
            firstStorage.open(chunkFileId, false, true);
            chunkFiles.add(chunkFileId);
            writeChunk(firstStorage, chunk, chunkFileId);
        }
        externalSortFiles(firstStorage, secondStorage, chunkFiles);
    }
    
    private int externalSortFiles(TemporaryStorage readStorage, TemporaryStorage writeStorage, List<Integer> files) throws IOException {
        if (files.size() <= readStorage.getSimultaneousAccessNumber()) {
            int output = writeStorage.create();
            mergeChunkFiles(readStorage, writeStorage, files, output);
            return output;
        } else {
            List<Path> chunkFiles = new ArrayList<>();
            throw new UnsupportedOperationException("Not implemented!");
        }
    }
    
    private void mergeChunkFiles(TemporaryStorage readStorage, TemporaryStorage writeStorage, List<Integer> input, int output) throws IOException {
        List<Pair<Integer, Phrase>> readers = new ArrayList<>();
        try {
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
   
    private void writeChunk(TemporaryStorage storage, Collection<Phrase> chunk, int ouputId) throws IOException {
        storage.open(ouputId, false, true);
        for (Phrase phase : chunk) {
            storage.write(ouputId, phase);
        }
        storage.close(ouputId);
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
                return new Phrase(line.substring(line.indexOf(':')), counter);
            }
            return null;
        }

        @Override
        public int write(int id, Phrase value) throws IOException {
            Triple<Path, BufferedReader, BufferedWriter> stored = files.get(id);
            assert stored != null;
            String strValue = value.asString();
            stored.third.write(strValue);
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
                tokenizer = new StringTokenizer(line, "|");
                if (tokenizer.hasMoreTokens()) {
                    return tokenizer.nextToken();
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
