package it.unipi.mircv.File;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

import static it.unipi.mircv.Config.*;

public class PLDescriptorFileHandler {
    private final RandomAccessFile randomAccessFile;
    private final FileChannel fileChannel;

    public PLDescriptorFileHandler() throws IOException {
        File file = new File(POSTING_LIST_DESC_FILE);
        if (file.exists()) {
            System.out.println("Posting List Descriptor file founded");
        } else {
            // Create the file
            if (file.createNewFile()) {
                System.out.println("Posting List Descriptor file created correctly");
            } else {
                System.out.println("Failed to create Posting List Descriptor file");
            }
        }

        randomAccessFile = new RandomAccessFile(POSTING_LIST_DESC_FILE,"rw");
        fileChannel = randomAccessFile.getChannel();
    }

    //this method will be called for each term in the conjunctive query in order to perform nextGEQ()
    public ArrayList<Integer> getMaxDocIds(int offset, int length) throws IOException {
        ArrayList<Integer> maxDocIds = new ArrayList<>();
        ByteBuffer maxDocIdsBuffer = ByteBuffer.allocate(length * DOC_ID_LENGTH);
        fileChannel.read(maxDocIdsBuffer, (long) offset * DOC_ID_LENGTH);
        for (int i = 0; i < length; i++){
            maxDocIdsBuffer.position(i * DOC_ID_LENGTH);
            maxDocIds.add(maxDocIdsBuffer.getInt());
        }
        return maxDocIds;
    }

    public void writeMaxDocIds(int offset, ArrayList<Integer> maxDocIds) throws IOException {
        ByteBuffer byteBuffer = ByteBuffer.allocate(maxDocIds.size() * DOC_ID_LENGTH);
        for (int maxDocId: maxDocIds) {
            byteBuffer.putInt(maxDocId);
        }
        byteBuffer.rewind();
        fileChannel.position((long) offset * DOC_ID_LENGTH);
        fileChannel.write(byteBuffer);

    }

    public void closeFileChannel() throws IOException {
        randomAccessFile.close();
        fileChannel.close();
    }
}
