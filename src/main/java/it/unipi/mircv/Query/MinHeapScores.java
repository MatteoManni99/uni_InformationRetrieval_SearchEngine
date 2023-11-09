package it.unipi.mircv.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.PriorityQueue;
import static it.unipi.mircv.Config.*;
public class MinHeapScores {

    //------------------Priority Queue of Doc Retrieved-----------------------//
    private final HashMap<Float, ArrayList<Integer>> score2DocIdMap;  //list of docIDs retrieved sorted by ranking
    private final PriorityQueue<Float> topScores;
    private int topDocCount; //counter for keep track of how many document have been inserted in the min-heap
    public MinHeapScores(){
        score2DocIdMap = new HashMap<>();
        topScores = new PriorityQueue<>();
        topDocCount =0;
    }
    private void insertDocIdInMap(float score,int docId){
        if (score2DocIdMap.containsKey(score)) {  //if score is present in hashmap
            score2DocIdMap.get(score).add(docId); //add element to the arrayList of docId
        }else{
            ArrayList<Integer> arrayList = new ArrayList<>();
            arrayList.add(docId);
            score2DocIdMap.put(score,arrayList);
        }
    }
    private void removeDocIdFromMap(float score){
        ArrayList<Integer> docIds = score2DocIdMap.get(score);
        if(docIds.size()>1){ //if there are more than 1 docIDs associated to the score then remove only one
            docIds.remove(docIds.size()-1);
        }
        else{ //if there is only one element then remove the tuple from the hashmap
            score2DocIdMap.remove(score);
        }
    }
    public void insertIntoPriorityQueue(float docScore, int minDocId){
        if (topDocCount < MAX_NUM_DOC_RETRIEVED){  //There less than k documents in the priority queue
            topDocCount++;
            System.out.println("Entra nell' if insertPriorityQueue"); //DEBUG
            try {
                topScores.add(docScore);
                insertDocIdInMap(docScore,minDocId);

            }catch(Exception e){
                System.out.println("Errore Non posso inserire doc:"+minDocId);
                e.printStackTrace();
            }

        }else{      //there are more than k documents in the priority queue

            if(docScore > topScores.peek()) { //need to check if minDocId should be inserted
                topScores.remove(topScores.peek());
                topScores.add(docScore);
                removeDocIdFromMap(topScores.peek());
                insertDocIdInMap(docScore,minDocId);
            }

        }
    }
    public int getTopDocCount(){return topDocCount; }
    public ArrayList<Integer> getDocId(float scores){return this.score2DocIdMap.get(scores);}

    public ArrayList<Integer> getTopDocIdReversed() {
        float score;
        ArrayList<Integer> topDocId = new ArrayList<Integer>();
        while(!topScores.isEmpty()){
            score = topScores.poll();
            topDocId.addAll(getDocId(score));
        }

        return topDocId;
    }
}