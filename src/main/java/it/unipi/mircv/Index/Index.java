package it.unipi.mircv.Index;

import ca.rmen.porterstemmer.PorterStemmer;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import static it.unipi.mircv.Config.*;

public class Index {
    private DocumentIndex documentIndex;
    private PorterStemmer stemmer = new PorterStemmer();
    int count = 0; //DEBUG
    private int numberOfBlocks;
    private int currentDocId;

    public Index(String fileCollectionPath) throws IOException {
        //this method remove precedent files
        cleanFolder("data");
        loadStopWordList();
        /*
        FileInputStream fis = new FileInputStream("collection.tar.gz");
        GZIPInputStream gzis = new GZIPInputStream(fis);
        InputStreamReader inputStreamReader = new InputStreamReader(gzis);
        BufferedReader reader = new BufferedReader(inputStreamReader);

        //System.out.println(reader.readLine()); // DEBUG eseguite questo se volete vedere i metadati della prima riga
        reader.mark(1024); // 1024 è quanti byte può leggere prima che il mark diventi non più valido
        String[] values = reader.readLine().split("\t"); //per vedere alla prima linea quanto sono lunghi i metadati
        reader.reset(); // riporto il reader all' inizio perché era andato alla riga successiva
        reader.skip(values[0].length()-1); // skip metadata
        */


        documentIndex = new DocumentIndex();
        currentDocId = 0;
        int blockID = 0;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(fileCollectionPath)); // vecchio reader prima della Compressed Reading
            while(reader!=null){
                System.out.println("BlockID: "+blockID); //DEBUG
                //singlePassInMemoryIndexing may stop for memory lack
                reader = singlePassInMemoryIndexing(blockID,reader);
                System.gc();
                blockID++;
            }
            numberOfBlocks = blockID;
        } catch (IOException e) {
            System.err.println("Error reading the file: " + e.getMessage());
        }
        documentIndex.addAverageDocumentLength();
    }

    public void loadStopWordList() {  // load in memory the lists of stop words from the json file
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            File file = new File("stop_words_english.json");
            stopWords = objectMapper.readValue(file, new TypeReference<>() {}); // Read the JSON file into a List

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void writeLexiconToBlock(Lexicon lexicon, int blockID) throws IOException {
        String path = "./data/";
        String fileLexicon = "lexicon" + blockID + ".dat";
        String fileDocIds = "docIds" + blockID+".dat";
        String fileTermFreq = "termFreq" + blockID+".dat";
        lexicon.toDisk(path,fileLexicon,fileDocIds,fileTermFreq);
    }

    private double freeMemoryPercentage() {
        long totalMemory = Runtime.getRuntime().totalMemory();
        long freeMemory = Runtime.getRuntime().freeMemory();
        return (double) freeMemory / totalMemory * 100;
    }

    private BufferedReader singlePassInMemoryIndexing(int blockID, BufferedReader reader) throws IOException {
        Lexicon lexicon = new Lexicon();

        BufferedReader readerToReturn = null;

        while (true) {
            count++; //DEBUG

            if(freeMemoryPercentage() < 20){
                //poor memory qt available -> break
                readerToReturn = reader;
                System.out.println("Memory leak! Free memory: "+ freeMemoryPercentage()); //DEBUG - print the memory available
                break;
            }
            //DEBUG per creare più di un blocco
            if (count == 3000 || count == 6000){
                readerToReturn = reader;
                System.out.println("blocco finito per debug");
                break;
            }
            String line = reader.readLine();
            if(line == null){
                //we reached the end of the file -> close file reader and break
                reader.close();
                break;
            }

            //DEBUG - every tot document print the memory available
            if (count%1000 == 0)
                System.out.println("Free memory percentage: "+ freeMemoryPercentage());

            //parsing and processing the document corresponding
            String[] values = line.split("\t"); //split document text and docID
            String[] tokens = Index.tokenization(values[1]);  //take tokens from the text
            String docNo = values[0];
            int docLength = processDocument(lexicon, tokens);
            documentIndex.add(docNo, docLength);
            //DEBUG
            if (count == 10000) break; //DEBUG
        }

        writeLexiconToBlock(lexicon, blockID);
        return readerToReturn;
    }

    private int processDocument(Lexicon lexicon, String[] tokens) {
        HashMap<String, Integer> wordCountDocument = new HashMap<>();
        int tokenCount = 0;

        //Count all occurrence of all terms in a document
        for (String token : tokens) {  //map with frequencies only
            if (stopWords.contains(token)) // stopWordRemoval
                continue;
            //token = stemmer.stemWord(token); // stemming
            if (token.length() > TERM_BYTES_LENGTH) // il token è più lungo di 64 byte quindi lo scartiamo
                continue;
            if (token != null){
                tokenCount++;
                if (wordCountDocument.get(token) == null)
                    wordCountDocument.put(token, 1);
                else
                    wordCountDocument.put(token, wordCountDocument.get(token) + 1);
            }
        }
        //updating the lexicon with the document processing results
        for (String term: wordCountDocument.keySet()) {
            lexicon.update(term, currentDocId , wordCountDocument.get(term));
        }
        currentDocId ++;

        return tokenCount;
    }
    public int getNumberOfBlocks() {
        return numberOfBlocks;
    }

    public static String findURLsExample(String inputString) {
        String pattern = "\\b(?:https?|ftp)://\\S+\\b"; // Matches URLs starting with http://, https://, or ftp://
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(inputString);
        ArrayList<String> tokens = new ArrayList<String>();
        while (m.find()) {
            String url = m.group();
            System.out.println("Match found: " + url);
        }
        return "";
    }

    public static String[] tokenization(String doc) {
        //html tags removal
        doc = doc.replaceAll("<[^>]*>", "");
        //punctuation and whitespace
        String result = doc.replaceAll("\\p{Punct}","").toLowerCase();
        String[] tokens = result.split("\\s+");

        return tokens;
    }

    private static void cleanFolder(String folderName) throws IOException {
        //function called every time the indexing starts in order to clean up the folder where blocks are stored
        File folder = new File(folderName);
        if(folder.exists() && folder.isDirectory()) {
            File[] files = folder.listFiles();

            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        if (file.delete()) System.out.println("File deleted: " + file.getName());
                        else System.err.println("It's impossible to delete the file: " + file.getName());
                    }
                }
            }
        } else Files.createDirectory(Paths.get("data"));
    }

    public DocumentIndex getDocumentIndex() {
        return documentIndex;
    }
}

