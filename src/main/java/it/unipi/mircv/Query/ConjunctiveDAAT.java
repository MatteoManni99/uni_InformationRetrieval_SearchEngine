package it.unipi.mircv.Query;

import it.unipi.mircv.File.DocumentIndexHandler;
import it.unipi.mircv.File.InvertedIndexHandler;
import it.unipi.mircv.File.LexiconHandler;
import it.unipi.mircv.File.SkipDescriptorFileHandler;
import it.unipi.mircv.Index.PostingListBlock;
import it.unipi.mircv.Index.SkipDescriptor;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import static it.unipi.mircv.Config.MIN_NUM_POSTING_TO_SKIP;
import static it.unipi.mircv.Config.POSTING_LIST_BLOCK_LENGTH;

public class ConjunctiveDAAT {
    private int numTermQuery;
    private final int[] docFreqs;
    private final int[] offsets;
    private final PostingListBlock[] postingListBlocks;
    private final SkipDescriptor[] skipDescriptors;
    private final DocumentIndexHandler documentIndexHandler;
    private final InvertedIndexHandler invertedIndexHandler;
    public ConjunctiveDAAT(String[] queryTerms) throws IOException {
        //initialize file handlers
        LexiconHandler lexiconHandler = new LexiconHandler();
        documentIndexHandler = new DocumentIndexHandler();
        invertedIndexHandler = new InvertedIndexHandler();
        SkipDescriptorFileHandler skipDescriptorFileHandler = new SkipDescriptorFileHandler();

        numTermQuery = queryTerms.length;

        //initialize arrays
        postingListBlocks = new PostingListBlock[numTermQuery];
        docFreqs =  new int[numTermQuery];
        offsets = new int[numTermQuery];
        skipDescriptors = new SkipDescriptor[numTermQuery];

        //search for the query terms in the lexicon
        for (int i = 0; i < numTermQuery; i++) {
            ByteBuffer entryBuffer = lexiconHandler.findTermEntry(queryTerms[i]);
            docFreqs[i] = lexiconHandler.getDf(entryBuffer);
            offsets[i] = lexiconHandler.getOffset(entryBuffer);

            if(docFreqs[i] > (MIN_NUM_POSTING_TO_SKIP * MIN_NUM_POSTING_TO_SKIP)){
                System.out.println("offsetToSkipSrittoNel Lexicon: " + lexiconHandler.getOffsetSkipDesc(entryBuffer));
                skipDescriptors[i] = skipDescriptorFileHandler.readSkipDescriptor(
                        lexiconHandler.getOffsetSkipDesc(entryBuffer), (int) Math.ceil(Math.sqrt(docFreqs[i])));
                postingListBlocks[i] = new PostingListBlock();
                postingListBlocks[i].setDummyFields(); // this to avoid using a boolean for the already read block optimization
            }
            else{
                skipDescriptors[i] = null;
                //load in main memory the posting list for which there is no skipDescriptor cause they are too small
                postingListBlocks[i] = invertedIndexHandler.getPostingList(offsets[i], docFreqs[i]);
            }
        }

        for (int i = 0; i < numTermQuery; i++){
            // break se non trovo il currentDocId in una delle altre posting list
            System.out.println("queryTerm: " + queryTerms[i] + " docFreq: " + docFreqs[i]);
        }
        //sort arrays for posting list Length
        sortArraysByArray(docFreqs, offsets, skipDescriptors, postingListBlocks);

        for (int i = 0; i < numTermQuery; i++){
            // break se non trovo il currentDocId in una delle altre posting list
            System.out.println("docFreq: " + docFreqs[i] + " offset: " + offsets[i] + " postList: " + i + postingListBlocks[i]);
        }
    }
    public ArrayList<Integer> processQuery() throws IOException {
        MinHeapScores heapScores = new MinHeapScores();

        int postingCount = 0;
        int currentDocId;
        int offsetNextGEQ;
        boolean continueWhile;

        System.out.println("docFreqs[0]: " + docFreqs[0]); //DEBUG

        while(postingCount < docFreqs[0]){

            if(skipDescriptors[0] != null){
                //load the first posting list block
                uploadPostingListBlock(0, postingCount, POSTING_LIST_BLOCK_LENGTH);
            }

            System.out.println(postingListBlocks[0]); //DEBUG

            //read an entire block for the shortest posting list and calculate the score for each document.
            do{
                currentDocId = postingListBlocks[0].getCurrentDocId();
                postingCount ++;
                float currentDocScore = 0;
                continueWhile = false;

                System.out.println("currentDocId: " + currentDocId);

                //calculate the partial score for the other posting list if they contain the currentDocId
                for (int i = 1; i < numTermQuery; i++) {
                    //if skipDescriptors[i] is not null load a posting list block by block
                    //DEBUG
                    if(skipDescriptors[i]== null) System.out.println("non ho lo skip: " + i);

                    if(skipDescriptors[i]!=null){
                        System.out.println("ho lo skip: " + i);
                        offsetNextGEQ = skipDescriptors[i].nextGEQ(currentDocId); // get the nextGEQ of the current posting list

                        // if the currentDocId is not in the posting list then break, otherwise search for it
                        if(offsetNextGEQ == -1){
                            System.out.println("break"); //DEBUG
                            continueWhile = true;
                            break;
                        }
                        else{
                            //calculate the skip size by the square root of the posting list length
                            int postingListSkipBlockSize = (int) Math.sqrt(docFreqs[i]);
                            //TODO controllare se va bene questa ottimizzazione
                            if ((postingListBlocks[i].getMaxDocID() > currentDocId                  // controllo il range del currentDocId per vedere
                                    && postingListBlocks[i].getMinDocID() < currentDocId) == false) // se siamo nello stesso blocco di prima
                                uploadPostingListBlock(i, (offsetNextGEQ - offsets[i]), postingListSkipBlockSize);
                        }

                    }

                    //search for the currentDocId posting list
                    if(currentDocIdInPostingList(i, currentDocId)){
                        int currentTf = postingListBlocks[i].getCurrentTf();
                        int documentLength = documentIndexHandler.readDocumentLength(
                                postingListBlocks[i].getCurrentDocId()
                        );
                        currentDocScore += ScoreFunction.BM25(currentTf,documentLength, docFreqs[i]);
                    }else{
                        continueWhile = true;
                        break;
                    }
                }
                if(continueWhile) continue;
                heapScores.insertIntoPriorityQueue(currentDocScore , currentDocId);
            } while(postingListBlocks[0].next() != -1);
        }
        return heapScores.getTopDocIdReversed();
    }

    private boolean currentDocIdInPostingList(int indexTerm, int currentDocId){

        System.out.println("entrato in currentDocIdInPostingList----------------------------------");
        do{
            System.out.println("currentDocId: " + currentDocId + " getCurrentDocId(): " + postingListBlocks[indexTerm].getCurrentDocId());
            if(postingListBlocks[indexTerm].getCurrentDocId() == currentDocId) return true;
            if(postingListBlocks[indexTerm].getCurrentDocId() > currentDocId) break;

        }while(postingListBlocks[indexTerm].next() != -1);
        return false;
    }
    private void uploadPostingListBlock(int indexTerm, int readElement, int blockSize) throws IOException {
        //Upload the posting list block
        //if the element to read are less in size than "blockSize", read the remaining elements
        //otherwise read a posting list block of size "blockSize"
        if (docFreqs[indexTerm] - readElement < blockSize) {
            postingListBlocks[0] = invertedIndexHandler.getPostingList(
                    offsets[indexTerm] + readElement,
                    docFreqs[indexTerm] - readElement
            );
        }
        else {
            postingListBlocks[indexTerm] = invertedIndexHandler.getPostingList(
                    offsets[indexTerm] + readElement,
                    blockSize);
        }
        if (docFreqs[indexTerm] - readElement < blockSize) {
            postingListBlocks[indexTerm] = invertedIndexHandler.getPostingList(
                    offsets[indexTerm] + readElement,
                    docFreqs[indexTerm] - readElement
            );
        }
        else {
            postingListBlocks[indexTerm] = invertedIndexHandler.getPostingList(
                    offsets[indexTerm] + readElement,
                    blockSize
            );
        }
    }
    public static void sortArraysByArray(int[] arrayToSort, int[] otherArray, SkipDescriptor[] otherOtherArray, PostingListBlock[] otherOtherOtherArray){
        // Sort all the input arrays according to the elements of the first array
        // Initialize an array of indexes to keep track of the original positions of the elements
        Integer[] indexes = new Integer[arrayToSort.length];
        for (int i = 0; i < indexes.length; i++) {
            indexes[i] = i;
        }
        // Sort the indexes array according to the elements of the array to sort
        Arrays.sort(indexes, Comparator.comparingInt(i -> arrayToSort[i]));
        // Apply the same permutation to the other arrays
        for (int i = 0; i < arrayToSort.length; i++) {
            if (indexes[i] != i) {
                //Change the elements in the array to sort
                int temp = arrayToSort[i];
                arrayToSort[i] = arrayToSort[indexes[i]];
                arrayToSort[indexes[i]] = temp;

               //Change the elements in the othersArray to sort
                temp = otherArray[i];
                otherArray[i] = otherArray[indexes[i]];
                otherArray[indexes[i]] = temp;

                SkipDescriptor tempSkipDescriptor = otherOtherArray[i];
                otherOtherArray[i] = otherOtherArray[indexes[i]];
                otherOtherArray[indexes[i]] = tempSkipDescriptor;

                PostingListBlock postingListBlock = otherOtherOtherArray[i];
                otherOtherOtherArray[i] = otherOtherOtherArray[indexes[i]];
                otherOtherOtherArray[indexes[i]] = postingListBlock;

                // Update the indexes array
                indexes[indexes[i]] = indexes[i];
            }
        }
    }

}
