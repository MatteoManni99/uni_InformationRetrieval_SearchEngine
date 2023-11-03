package it.unipi.mircv;

import it.unipi.mircv.Index.Compression;

import java.io.IOException;


public class TestMatteo {
    public static void main(String[] args) throws IOException {

        /*
        //test DocumentIndexHandler
        Index index = new Index("test_collection.tsv");
        int numberOfBlocks = index.getNumberOfBlocks();
        BlockMerger blockMerger = new BlockMerger(numberOfBlocks);
        blockMerger.mergeBlocks();

        DocumentIndexHandler documentIndexHandler = new DocumentIndexHandler();
        System.out.println("document length: " + documentIndexHandler.readDocumentLength(3));
        documentIndexHandler.closeFileChannel();
        */

        // test compression
        byte[] intCompressed1 = Compression.variableByteCompression(1000);
        byte[] intCompressed2 = Compression.variableByteCompression(10023402);

        Compression.printBytes(Compression.reverseArray(intCompressed1));
        Compression.printBytes(Compression.reverseArray(intCompressed2));

        System.out.println(Compression.variableByteDecompression(intCompressed1));
        System.out.println(Compression.variableByteDecompression(intCompressed2));
    }


}
