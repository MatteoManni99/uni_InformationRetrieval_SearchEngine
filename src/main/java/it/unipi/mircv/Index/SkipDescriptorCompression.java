package it.unipi.mircv.Index;
import java.util.ArrayList;

public class SkipDescriptorCompression {
    private final ArrayList<Integer> maxDocIds;
    private final ArrayList<Long> offsetMaxDocIds;
    private final ArrayList<Integer> numByteMaxDocIds;
    private final ArrayList<Long> offsetTermFreqs;
    private final ArrayList<Integer> numByteTermFreqs;


    public SkipDescriptorCompression(){
        maxDocIds = new ArrayList<>();
        offsetMaxDocIds = new ArrayList<>();
        numByteMaxDocIds = new ArrayList<>();
        offsetTermFreqs = new ArrayList<>();
        numByteTermFreqs = new ArrayList<>();
    }
    public int size(){
        return maxDocIds.size();
    }
    public void add(int maxDocId, long offsetDocId, int byteLengthDocId, long offsetTermFreq, int byteTermFreq){
        maxDocIds.add(maxDocId);
        offsetMaxDocIds.add(offsetDocId);
        numByteMaxDocIds.add(byteLengthDocId);
        offsetTermFreqs.add(offsetTermFreq);
        numByteTermFreqs.add(byteTermFreq);
    }
    public ArrayList<Integer> getMaxDocIds(){
        return maxDocIds;
    }
    public ArrayList<Long> getOffsetMaxDocIds(){
        return offsetMaxDocIds;
    }
    public ArrayList<Integer> getNumByteMaxDocIds() {
        return numByteMaxDocIds;
    }

    public ArrayList<Long> getOffsetTermFreqs() {
        return offsetTermFreqs;
    }

    public ArrayList<Integer> getNumByteTermFreqs() {
        return numByteTermFreqs;
    }

    public long[] nextGEQ(int docId){
        /*for(int i = 0; i < maxDocIds.size(); i++)
            if(maxDocIds.get(i) > docId) return offsetMaxDocIds.get(i);
        return -1;
        */
        long[] toReturn = new long[2];

        // Custom binary search to find the index of the first integer greater than the input
        int low = 0;
        int high = maxDocIds.size();
        while (low < high)
        {
            int mid = low + (high - low) / 2;
            long midValue = maxDocIds.get(mid);
            if (midValue <= docId)
                low = mid + 1; // Discard the left half
            else
                high = mid; // Include the current mid index in the search space
        }
        // Check if the index is within the bounds of the list
        //return (low < maxDocIds.size()) ? offsetMaxDocIds.get(low) : -1;
        if (low < maxDocIds.size()) {
            toReturn[0] = offsetMaxDocIds.get(low);
            toReturn[1] = offsetTermFreqs.get(low);
        } else {
            toReturn[0] = -1; //TODO si poò mettere un if prima della ricerca binaria
        }
        return toReturn;
    }

    @Override
    public String toString(){
        String stringToReturn = "";
        for (int i = 0; i <maxDocIds.size(); i++){
            stringToReturn += "maxDocId: " + maxDocIds.get(i) + " offsetDocId: " + offsetMaxDocIds.get(i) + " offsetTermFreq: " + offsetTermFreqs.get(i) + "\n";
        }
        return stringToReturn;
    }
}
