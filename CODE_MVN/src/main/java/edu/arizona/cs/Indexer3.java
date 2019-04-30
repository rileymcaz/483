/**
 * implements the lucene standard analyzer in the system and lemmatization to get stemming effects of the porter stemmer
 */
package edu.arizona.cs; //DO NOT CHANGE THE PACKAGE IS CORRECT

//java packages
import java.io.File;
import java.util.Scanner;
import java.io.IOException;
import java.nio.file.Paths;

//lucene
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;


//indexes documents in the path
public class Indexer3{
    private static String p = "target/classes/wiki-subset-20140602";
    
    public static void main(String[] args) throws Exception{
        if(args.length >= 1){
            StandardAnalyzer analyzer = new StandardAnalyzer(); //runs stemming and lemmas from lucene
            Directory index = FSDirectory.open(Paths.get("target/INDEX3"));
            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            IndexWriter w = new IndexWriter(index, config);
            File folder = new File(p);
            File[] listOfFiles = folder.listFiles();
            
            for(File filename : listOfFiles){
                parse(filename.getName(), w);
                System.out.println("Finished indexing File:   \t" + filename);
            }
            w.close();
            index.close();
        }
        else{
            System.out.println("No args");
        }
        System.out.println("Finished indexing");
    }
    
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
            //Summary
            else {
                //removes the equal signs from categories score 7
                if(line.contains("==")){
                    line.replace("=", "");
                    para += " " + line;
                }
                //adds categories
                else if(line.contains("CATEGORIES:")){
                    cats += " " + line;
                }
                //removes noise
                else if(line.contains("[tpl]")){
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
                }
                //adds line and doesn't trip flag
                else{
                    para += " " + line;
                }
            }
        }
        scan.close();
    }

    private static void addDoc(IndexWriter w, String docID, String text, String cats) throws IOException {        
        //creates new document
        Document doc = new Document();

        //adds the text of the document
        doc.add(new TextField("text", text.toLowerCase(), Field.Store.YES));

        //adds categories
        doc.add(new TextField("category", cats.toLowerCase(), Field.Store.YES));

        //adds the docID
        doc.add(new TextField("title", docID.toLowerCase(), Field.Store.YES));
        
        //adds document to the index
        w.addDocument(doc);
    }
}