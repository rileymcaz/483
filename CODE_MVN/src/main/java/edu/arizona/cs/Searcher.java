/**
 * Riley McGarity
 * This is my main searcher and is used to find the best answers to wiki searches
 */
package edu.arizona.cs; //DO NOT CHANGE THE PACKAGE IS CORRECT NO MATTER WHAT VS CODE SAYS

//Java
import java.io.File;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

//core nlp
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.ling.CoreAnnotations.*;
import edu.stanford.nlp.ling.CoreLabel;

//lucene
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;

//This is the searcher class which is used to 
public class Searcher{
    //locations of the questions
    private static String path = "target/classes/questions.txt";
    //allows you to see more hits on a page
    private static int hitsPerPage = 1;

    //runs the search
    public static void main(String args[]) throws Exception{
        //sets up index
        StandardAnalyzer analyzer = new StandardAnalyzer();
        Directory index = FSDirectory.open(Paths.get("target/INDEX"));//edit directory type
        
        //BM25 params
        //score 26 k = 0.6 b = 0
        float k = (float) 0.6;
        float b = 0;
        
        File file = new File(path); //access using realtive path to resources
        Scanner scan = new Scanner(file);
        int total = 0;
        int questions = 0;
        while(scan.hasNextLine()){
            String line = scan.nextLine();
            if(line.length() == 0){
                continue;
            }
            else{
                //gets category and preps it for the search
                String cat = line;
                System.out.println("In the Category of: " + cat);
                cat = cat.replaceAll("\\p{P}", "");
                cat = getlemms(cat);

                //gets the question and preps it for the search
                String question = scan.nextLine();
                System.out.println("For The Question of: " + question);
                question = question.replaceAll("\\p{P}", "");
                question = getlemms(question);
                
                //gets answer from the file
                String answer = scan.nextLine();
                answer = answer.replaceAll("\\p{P}", "");
                
                //gets the score
                System.out.println("Actual Answer:\t" + answer);
                int score = quer(question, analyzer, index, answer, cat, k, b);
                System.out.println("Score:\t" + score);
                System.out.println();

                questions += 1;
                total += score;
            }
        }
        System.out.println();
        System.out.println("Total quesitons answered correctly:\t" + total);
        System.out.println("Total quesitons:\t\t\t" + (questions * hitsPerPage));
        scan.close();
        index.close();
    }

    //lemmatizes a string
    private static String getlemms(String q){
        String lemtext = "";

        //lemmatize and stem text q
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, pos, lemma");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

        //ADDS DOCUMENT
        Annotation document = new Annotation(q);
        pipeline.annotate(document);

        List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);
        for (CoreMap sentence : sentences) {
            for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
                lemtext += token.get(LemmaAnnotation.class) + " "; //concats lemmas

            }
        }
        return lemtext;
    }

    //runs the query on the indecx
    private static int quer (String querystr, StandardAnalyzer analyzer, Directory index, String answer, String category, float k, float b) throws Exception{
        //current score against query
        int score = 0;

        // generates query
        querystr = querystr.trim();
        
        //removing stop words score 25 without removal 22
        querystr = rmvstopwords(querystr);
        
        //25 without category 18 with category
        querystr = "text:("+ querystr + ") NOT title:(" + querystr + ")";
        //System.out.println(querystr);


        Query q = new QueryParser("text", analyzer).parse(querystr);

        // 3. search
        IndexReader reader = DirectoryReader.open(index);
        IndexSearcher searcher = new IndexSearcher(reader);
        
        /*----------------------------------------------------------------*/

        //sets to BM25 24
        searcher.setSimilarity(new BM25Similarity(k, b));

        //sets to TFIDF score 2
        //searcher.setSimilarity(new ClassicSimilarity());
        
        /*----------------------------------------------------------------*/
        
        TopDocs docs = searcher.search(q, hitsPerPage);
        ScoreDoc[] hits = docs.scoreDocs;

        // 4. display results
        for(int i=0;i<hits.length;++i) {
            int docId = hits[i].doc;
            Document d = searcher.doc(docId);
            String result = d.get("title");
            if(compar(result, answer) && score == 0){
                System.out.println("The searcher returned Answer: What is " + result + "\t With a score of: " + searcher.explain(q, docId).getValue());
                score = hitsPerPage - i;
                break;
            }

            System.out.println("The searcher returned Answer: What is " + result + "\t With a score of: " + searcher.explain(q, docId).getValue());
        }
        reader.close();
        return score;
    }

    //removes the stopwords
    private static String rmvstopwords(String query){
        String[] words = query.split(" ");
        for(int i = 0; i < words.length; i++){
            Scanner scan = null;
            try{
                File file = new File("src/main/resources/stopwords.txt"); //access using realtive path to resources
                scan = new Scanner(file);
                while(scan.hasNext()){
                    if(scan.next().trim().equals(words[i].toLowerCase())){
                        words[i] = "";
                    }
                }

            } catch(Exception e){
                e.printStackTrace();
            }
            finally{
                scan.close();
            }
        }
        return String.join(" ", words);
    }

    //does a comparrison of the answers and returns if it passed or failed
    private static Boolean compar(String one, String two){
        //removes whitespace
        String shortone = one.trim();
        String shorttwo = two.trim();
        shortone = shortone.replaceAll("\\s+", "");
        shorttwo = shorttwo.replaceAll("\\s+", "");
        String[] answersone;
        String[] answerstwo;
        if(shorttwo.contains("|")){
            shorttwo = shorttwo.replace("|", "OR");
            answerstwo = shorttwo.split("OR");
        }
        else{
            answerstwo = new String[] {shorttwo};
        }
        if(shortone.contains("(")){
            shortone = shortone.replace("(", "OR");
            answersone = shortone.split("OR");
        }
        else{
            answersone = new String[] {shortone};
        }

        
        for(String answerone : answersone){
            for(String answertwo : answerstwo){
                //removes punctuation
                answertwo = answertwo.replaceAll("\\p{P}", "");
                answerone = answerone.replaceAll("\\p{P}", "");
                
                //removes case
                answertwo = answertwo.toLowerCase();            
                answerone = answerone.toLowerCase();
                if(answertwo.equals(answerone)){
                    return true;
                }
            }
        }
        return false;
    }
}