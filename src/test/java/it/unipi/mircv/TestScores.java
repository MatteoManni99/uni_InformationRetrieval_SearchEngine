package it.unipi.mircv;


import it.unipi.mircv.evaluation.SystemEvaluator;
import it.unipi.mircv.file.DocumentIndexFileHandler;
import it.unipi.mircv.file.InvertedIndexFileHandler;
import it.unipi.mircv.file.LexiconFileHandler;
import it.unipi.mircv.index.BlockMerger;
import it.unipi.mircv.index.BlockMergerCompression;
import it.unipi.mircv.index.Index;
import it.unipi.mircv.index.PostingListBlock;
import it.unipi.mircv.query.MinHeapScores;
import it.unipi.mircv.query.ScoreFunction;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static it.unipi.mircv.Config.STARTING_PATH;
import static it.unipi.mircv.Parameters.*;
import static it.unipi.mircv.Parameters.QueryProcessor.*;
import static it.unipi.mircv.Parameters.Score.BM25;
import static it.unipi.mircv.Parameters.Score.TFIDF;
import static it.unipi.mircv.Utils.printFilePaths;
import static it.unipi.mircv.Utils.setFilePaths;

public class TestScores {

    @Test
    void buildIndexForTest() throws IOException {
        // compute the index on which the score functions are going to be tested
        flagStemming = false;
        flagStopWordRemoval = true;
        flagCompressedReading = false;
        STARTING_PATH = "dataForScoreTest";
        setFilePaths();
        printFilePaths();

        Index index = new Index(STARTING_PATH + '/',"test_collection_for_query.tsv",false);

        BlockMerger blockMerger = new BlockMerger();
        blockMerger.mergeBlocks(index.getNumberOfBlocks());


        //QUERY ------
        DocumentIndexFileHandler documentIndexFileHandler = new DocumentIndexFileHandler();
        collectionSize = documentIndexFileHandler.readCollectionSize();
        avgDocLen = documentIndexFileHandler.readAvgDocLen();
        scoreType = TFIDF;
        docsLen = documentIndexFileHandler.loadAllDocumentLengths();

        SystemEvaluator.queryResult("Manhattan", DISJUNCTIVE_DAAT);
    }

    @Test
    void testTFIDF() throws IOException {
        // compute the actual scores w.r.t TFIDF for the docIds in the collection and compare them with the ones computed with
        // our method to process the querys (in Disjunctive DAAT mode)
        flagStemming = false;
        flagStopWordRemoval = true;
        flagCompressedReading = false;
        STARTING_PATH = "dataForScoreTest";
        setFilePaths();
        printFilePaths();
        DocumentIndexFileHandler documentIndexFileHandler = new DocumentIndexFileHandler();
        collectionSize = documentIndexFileHandler.readCollectionSize();
        avgDocLen = documentIndexFileHandler.readAvgDocLen();
        scoreType = TFIDF;
        docsLen = documentIndexFileHandler.loadAllDocumentLengths();
        printCollectionStatistics();
        Utils.loadStopWordList();
        HashMap<Float, ArrayList<Integer>> score2DocIdMap;

        // evaluate the query
        String[] querys = new String[]{"Manhattan project"};
        score2DocIdMap = SystemEvaluator.queryResultForTest(querys[0],DISJUNCTIVE_DAAT);

        // To compute the scores, WolframAplha was used
        Assertions.assertEquals(new ArrayList<>(List.of(2)),score2DocIdMap.get(0.39794f)); // computed score for docId = 2
        Assertions.assertEquals(new ArrayList<>(List.of(3)),score2DocIdMap.get(1.0406106f)); // computed score for docId = 3
        Assertions.assertEquals(new ArrayList<>(List.of(0,8)),score2DocIdMap.get(0.9208187f)); // computed score for docId = 2 and docId = 8

        // evaluate the query
        querys = new String[]{"science secret"};
        score2DocIdMap = SystemEvaluator.queryResultForTest(querys[0],DISJUNCTIVE_DAAT);

        // To compute the scores, WolframAplha was used
        Assertions.assertEquals(new ArrayList<>(List.of(1,2)),score2DocIdMap.get(0.69897f)); // computed score for docId = 1 and docId = 2
        Assertions.assertEquals(new ArrayList<>(List.of(8)),score2DocIdMap.get(1.0f)); // computed score for docId = 8

        // evaluate the query
        querys = new String[]{"into the ocean"};
        score2DocIdMap = SystemEvaluator.queryResultForTest(querys[0],DISJUNCTIVE_DAAT);

        // To compute the scores, WolframAplha was used
        Assertions.assertEquals(new ArrayList<>(List.of(9)),score2DocIdMap.get(1.0f)); // computed score for docId = 9

        System.out.println("test on the method TFIDF --> SUCCESSFUL");
    }

    @Test
    void testBM25() throws IOException {
        // compute the actual scores w.r.t BM25 for the docIds in the collection and compare them with the ones computed with
        // our method to process the querys (in Disjunctive DAAT mode)
        flagStemming = false;
        flagStopWordRemoval = true;
        flagCompressedReading = false;
        STARTING_PATH = "dataForScoreTest";
        setFilePaths();
        printFilePaths();
        DocumentIndexFileHandler documentIndexFileHandler = new DocumentIndexFileHandler();
        collectionSize = documentIndexFileHandler.readCollectionSize();
        avgDocLen = documentIndexFileHandler.readAvgDocLen();
        scoreType = BM25;
        docsLen = documentIndexFileHandler.loadAllDocumentLengths();
        printCollectionStatistics();
        Utils.loadStopWordList();
        HashMap<Float, ArrayList<Integer>> score2DocIdMap;

        // evaluate the query
        String[] querys = new String[]{"Manhattan project"};
        score2DocIdMap = SystemEvaluator.queryResultForTest(querys[0],DISJUNCTIVE_DAAT);

        // To compute the scores, WolframAplha was used
        Assertions.assertEquals(new ArrayList<>(List.of(0)),score2DocIdMap.get(0.4060303f)); // computed score for docId = 0
        Assertions.assertEquals(new ArrayList<>(List.of(2)),score2DocIdMap.get(0.19046701f)); // computed score for docId = 2
        Assertions.assertEquals(new ArrayList<>(List.of(3)),score2DocIdMap.get(0.37519884f)); // computed score for docId = 3
        Assertions.assertEquals(new ArrayList<>(List.of(8)),score2DocIdMap.get(0.37639308f)); // computed score for docId = 8

        // evaluate the query
        querys = new String[]{"science secret"};
        score2DocIdMap = SystemEvaluator.queryResultForTest(querys[0],DISJUNCTIVE_DAAT);

        // To compute the scores, WolframAplha was used
        Assertions.assertEquals(new ArrayList<>(List.of(1)),score2DocIdMap.get(0.24931414f)); // computed score for docId = 1
        Assertions.assertEquals(new ArrayList<>(List.of(2)),score2DocIdMap.get(0.33454975f)); // computed score for docId = 2
        Assertions.assertEquals(new ArrayList<>(List.of(8)),score2DocIdMap.get(0.40875915f)); // computed score for docId = 8

        // evaluate the query
        querys = new String[]{"into the ocean"};
        score2DocIdMap = SystemEvaluator.queryResultForTest(querys[0],DISJUNCTIVE_DAAT);

        // To compute the scores, WolframAplha was used
        Assertions.assertEquals(new ArrayList<>(List.of(9)),score2DocIdMap.get(0.3163842f)); // computed score for docId = 9

        System.out.println("test on the method BM25 --> SUCCESSFUL");
    }

    private static void printCollectionStatistics() {   // print AverageDocumentLength, CollectionSize and the
                                                        // document lengths of the collection
        System.out.println("avgDocLen = " + avgDocLen);
        System.out.println("collectionSize = " + collectionSize);
        for (int i = 0; i < 10; i++)
            System.out.println("docLen[" + i + "] = " + docsLen[i]);
    }
}
