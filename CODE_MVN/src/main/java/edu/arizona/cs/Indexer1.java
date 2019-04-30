/**
 * This indexer runs no smarts except on parsing the documents for data
 * it does no stemming or lemmetization simply adds each word on 
 * whitespace to the index in each document.
 */

package edu.arizona.cs; //DO NOT CHANGE THE PACKAGE IS CORRECT

//java packages
import java.io.File;
import java.util.Scanner;
import java.io.IOException;
import java.nio.file.Paths;

import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
//lucene
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

//indexes documents in the path
public class Indexer1{
    private static String p = "target/classes/wiki-subset-20140602";
    
    /**
     * uses a whitespace analyser and no lemmitization
     * 
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception{
        if(args.length >= 1){
            WhitespaceAnalyzer analyzer = new WhitespaceAnalyzer(); //no stemming or lemmas
            Directory index = FSDirectory.open(Paths.get("target/INDEX1"));//edit directory type
            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            IndexWriter w = new IndexWriter(index, config);
            File folder = new File(p);
            File[] listOfFiles = folder.listFiles();
            for(File filename : listOfFiles){
                parse(filename.getName(), w);
                System.out.println("Finished indexing File: " + filename);
            }
            w.close();
            index.close();
        }
        else{
            System.out.println("No args");
        }
        System.out.println("Finished indexing");
    }

    //parses a file and adds it to the index
    private static void parse(String filename, IndexWriter w) throws Exception{
        File file = new File(p + "/" + filename); //access using realtive path to resources
        Scanner scan = new Scanner(file); 
        String title = "";
        String para = "";
        String cats = "";
        while(scan.hasNextLine()){
            String line = scan.nextLine();
            //title
            if(line.length() > 4 && line.charAt(0) == '[' && line.charAt(1) == '[' && line.contains("]]")){
                //new title
                if(title.equals("")){
                    title = line.replace("[", "").replace("]", "");
                    para = title;
                    cats = "";
                }
                //old title
                else{
                    // creates new item in the index
                    addDoc(w, title, para, cats);
                    title = line.replace("[", "").replace("]", "");
                    para = title;
                    cats = "";
                }
            }
            //blank lines
            else if(line.length() == 0){
                continue;  
            }
            //content
            else {
                //removes the equal signs from categories
                if(line.contains("==")){
                    line.replace("=", "");
                    para += " " + line;
                }
                //adds categories
                else if(line.contains("CATEGORIES:")){
                    cats += " " + line;
                }
                //removes noise
                /*else if(line.contains("[tpl]")){
                    line = line.replace("[tpl]", "START DEL ");
                    line = line.replace("[/tpl]", "END");
                    String[] lines = line.split("START | END");
                    for (int i = 0 ; i < lines.length ; i ++ ){
                        if(lines[i].split(" ").length >= 1){
                            if(!lines[i].split(" ")[0].equals("DEL")){
                                para += " " + lines[i];
                            }
                        }
                    }
                }*/
                //adds line and doesn't trip flag
                else{
                    para += " " + line;
                }
            }
        }
        scan.close();
    }

    //adds a document to the index
    private static void addDoc(IndexWriter w, String docID, String text, String cats) throws IOException {
        //cleans up text
        text = text.toLowerCase().replaceAll("\\p{P}", "");
        cats = cats.toLowerCase().replaceAll("\\p{P}", "");
        docID = docID.toLowerCase().replaceAll("\\p{P}", "");
        
        //creates new document
        Document doc = new Document();

        //adds the text of the document
        doc.add(new TextField("text", text, Field.Store.YES));

        //adds categories
        doc.add(new TextField("category", cats, Field.Store.YES));

        //adds the docID
        doc.add(new TextField("title", docID, Field.Store.YES));
        
        //adds document to the index
        w.addDocument(doc);
    }
}