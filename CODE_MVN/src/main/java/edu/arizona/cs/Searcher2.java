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
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
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

public class Searcher2{
    private static String path = "target/classes/questions.txt";
    private static int hitsPerPage = 1;
    public static void main(String args[]) throws Exception{
        //sets up index
        WhitespaceAnalyzer analyzer = new WhitespaceAnalyzer();
        Directory index = FSDirectory.open(Paths.get("target/INDEX2"));//edit directory type
        float k = (float) 0.7;
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
                String cat = line;
                cat = cat.replaceAll("\\p{P}", "");
                String question = scan.nextLine();
                System.out.println("For The Query: " + question);
                question = question.replaceAll("\\p{P}", "");
                question = getlemms(question);
                String answer = scan.nextLine();
                answer = answer.replaceAll("\\p{P}", "");
                int score = quer(question, analyzer, index, answer, cat, k, b);
                System.out.println("Answer:\t" + answer);
                System.out.println("Score:\t" + score);
                System.out.println();

                questions += 1;
                total += score;
            }
        }
        System.out.println();
        System.out.println("K: " + k + "\tB: " + b);
        System.out.println("Total points:\t" + total);
        System.out.println("Possible points:\t" + (questions * hitsPerPage));
        scan.close();
        index.close();
    }

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

    private static int quer (String querystr, WhitespaceAnalyzer analyzer, Directory index, String answer, String category, float k, float b) throws Exception{
        //current score against query
        int score = 0;

        // generates query
        querystr.trim();
        
        //removing stop words score 24 without removal 21
        querystr = rmvstopwords(querystr);
        
        //22
        querystr = "text:("+ querystr + ") NOT title:(" + querystr + ")";

        System.out.println("The Actual query: " + querystr);

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
                System.out.println((i + 1) + ". " + result + "\t" + searcher.explain(q, docId).getValue());
                score = hitsPerPage - i;
                break;
            }

            System.out.println((i + 1) + ". " + result + "\t" + searcher.explain(q, docId).getValue());
        }
        reader.close();
        return score;
    }

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
                System.out.println(answerone + "\n" + answertwo);
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