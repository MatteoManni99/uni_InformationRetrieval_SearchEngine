package it.unipi.mircv;
import it.unipi.mircv.File.DocumentIndexHandler;
import it.unipi.mircv.Index.BlockMerger;
import it.unipi.mircv.Index.Index;
import it.unipi.mircv.Query.QueryProcessor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

import static it.unipi.mircv.Config.COLLECTION_FILE;

public class CommandLineInterface {

    public static void main( String[] args ) throws IOException {

        String commandList = "command list: \n" +
                "list --> print this command list\n" +
                "index (c) --> perform indexing with compression or not\n" +
                "query --> (hopefully) return most N relevant docIds\n" +
                "quit";
        System.out.println(commandList);

        Scanner scanner = new Scanner(System.in);

        boolean quit = false;
        while(!quit){

            System.out.print("enter a command: ");
            String[] command = scanner.nextLine().split("\\s+");

            switch (command[0]) {
                case "index":
                {
                    if (command.length>1){
                        if(command[1].charAt(0) == 'c'){
                            System.out.println("indexing with compression...");
                            long startTime = System.currentTimeMillis();

                            //TODO add indexing with compression

                            long endTime = System.currentTimeMillis();
                            long elapsedTime = endTime - startTime;
                            System.out.println("indexing with compression finished in " + (float)elapsedTime/1000 +"sec");
                            break;
                        } //else don't break

                    } else {
                        System.out.println("indexing...");
                        long startTime = System.currentTimeMillis();

                        //TODO add indexing
                        Index index = new Index(COLLECTION_FILE);
                        int numberOfBlocks = index.getNumberOfBlocks();
                        BlockMerger blockMerger = new BlockMerger(numberOfBlocks);
                        blockMerger.mergeBlocks();

                        long endTime = System.currentTimeMillis();
                        long elapsedTime = endTime - startTime;
                        System.out.println("indexing finished in " + (float)elapsedTime/1000 +"sec");
                        break;
                    }
                }

                case "query":
                {
                    //query terms are in command[1:length-1]
                    System.out.println("number of terms: " + (command.length - 1));
                    long startTime = System.currentTimeMillis();

                    //TODO add query processing

                    // ---------------------TEST DAAT-----------------------------
                    String query = "solis";
                    QueryProcessor queryProcessor = new QueryProcessor(query);
                    ArrayList<Integer> docId = queryProcessor.DAAT();
                    System.out.println("Doc Id retrieved: ");
                    System.out.println(docId);

                    long endTime = System.currentTimeMillis();
                    long elapsedTime = endTime - startTime;
                    System.out.println("query processed in " + (float)elapsedTime/1000 +"sec");
                    break;
                }

                case "list":
                    System.out.println(commandList);
                    break;

                case "quit":
                    quit = true;
                    break;

                default:
                    System.out.println("unknown command");
                    break;
            }
        }
        scanner.close();
    }
}