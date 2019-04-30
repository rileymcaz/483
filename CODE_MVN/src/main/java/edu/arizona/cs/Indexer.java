/**
 * Riley McGarity
 * Implements the best indexer possible
 */
package edu.arizona.cs; //DO NOT CHANGE THE PACKAGE IS CORRECT

//java packages
import java.io.File;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.io.IOException;
import java.nio.file.Paths;

//lucene
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

//core nlp
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.ling.CoreAnnotations.*;
import edu.stanford.nlp.ling.CoreLabel;

//indexes documents in the path
public class Indexer{
    //location of wiki directory
    private static String p = "target/classes/wiki-subset-20140602";
    
    //reads the files and parses them one at a time.
    public static void main(String[] args) throws Exception{
        if(args.length >= 1){
            StandardAnalyzer analyzer = new StandardAnalyzer();
            Directory index = FSDirectory.open(Paths.get("target/INDEX"));
            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            IndexWriter w = new IndexWriter(index, config);
            File folder = new File(p);
            File[] listOfFiles = folder.listFiles();
            
            for(File filename : listOfFiles){
                System.out.println("Working on indexing File: \t" + filename);
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
    
    //parses a file and adds it to the index
    private static void parse(String filename, IndexWriter w) throws Exception{
        File file = new File(p + "/" + filename); //access using realtive path to resources
        Scanner scan = new Scanner(file); 
        boolean flag = true;
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
                    flag = true;
                    cats = "";
                }
                //old title
                else{
                    // creates new item in the index
                    addDoc(w, title, para, cats);
                    title = line.replace("[", "").replace("]", "");
                    para = title;
                    flag = true;
                    cats = "";
                }
            }
            //blank lines
            else if(line.length() == 0){
                continue;  
            }
            //content
            else if(flag) {
                //only records the first paragraph
                if(line.contains("==")){
                    flag = false;
                }
                //adds categories
                else if(line.contains("CATEGORIES:")){
                    cats += " " + line;
                }
                //adds line and doesn't trip flag
                else{
                    para += " " + line;
                }
            }
        }
        scan.close();
    }

    //adds a document with lemmas
    private static void addDoc(IndexWriter w, String docID, String text, String cats) throws IOException {
        String lemtext = "";
        
        //creates new document
        Document doc = new Document();

        //lemmatize and stem text
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, pos, lemma");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

        //ADDS DOCUMENT
        Annotation document = new Annotation(text);
        pipeline.annotate(document);

        List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);
        for (CoreMap sentence : sentences) {
            for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
                lemtext += token.get(LemmaAnnotation.class) + " "; //concats lemmas

            }
        }

        //adds the text of the document
        doc.add(new TextField("text", lemtext.toLowerCase(), Field.Store.YES));

        //adds categories
        doc.add(new TextField("category", cats.toLowerCase(), Field.Store.YES));

        //adds the docID
        doc.add(new TextField("title", docID.toLowerCase(), Field.Store.YES));
        
        //adds document to the index
        w.addDocument(doc);
    }
}