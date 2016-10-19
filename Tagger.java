import java.io.*;
import java.util.*;

/**
 * Created by woyelin on 3/2/16.
 */

public class Tagger {

        private static final String STPOS = "ST";
        private static final String STTOKEN = "###";
        private static final String EDPOS = "ED";
        private static final String EDTOKEN = "$$$";
        private static final String UNKNOWN = "???";

        public static void viterbi(ArrayList<String> s, BufferedWriter bw, HashMap<String, HashMap<String, Double>> trans, HashMap<String, HashMap<String, Double>> emits, HashMap<String, HashSet<String>> wordTag )  {

                /*    dp(i, u, v) = dp(i-1, w, u) x q(v|w, u) x e(token|v);    */

                // first KEY: index of current word
                // second KEY: prePos
                // third KEY: curPos
                // VALUE: probability
                // initialize dp(1, *, *) = 1;
                HashMap<Integer, HashMap<String, HashMap<String, Double>>> dp = new HashMap<>();
                dp.put(1, new HashMap<String, HashMap<String, Double>>());
                dp.get(1).put(STPOS, new HashMap<String, Double>());
                dp.get(1).get(STPOS).put(STPOS, 1.0);

                // back pointer
                // first KEY: index of current word
                // second KEY: prePos
                // third KEY: curPos
                // VALUE: prePreState
                // initialize: bp(1, *, *) = null;
                HashMap<Integer, HashMap<String, HashMap<String, String>>> bp = new HashMap<>();

                for (int i = 2; i < s.size(); i++) {
                        HashSet<String> curPosSet = wordTag.get(s.get(i));
                        HashSet<String> prePosSet = wordTag.get(s.get(i - 1));
                        HashSet<String> prePrePosSet = wordTag.get(s.get(i - 2));

                        // create entry for word senten[i] in "dp" "bp"
                        dp.put(i, new HashMap<String, HashMap<String, Double>>());
                        bp.put(i, new HashMap<String, HashMap<String, String>>());

                        System.out.println("db 47: i = " + i + " "+s.get(i-2) + " " + s.get(i-1) + " "+ s.get(i));

                        double maxRec = 0.0;

                        for (String curPos : curPosSet) {
                                for (String prePos : prePosSet) {
                                        // create entry: dp(i, prePrePos, prePos);
                                        dp.get(i).put(prePos, new HashMap<String, Double>());
                                        dp.get(i).get(prePos).put(curPos, 0.0);
                                        bp.get(i).put(prePos, new HashMap<String, String>());
                                        bp.get(i).get(prePos).put(curPos, null);

                                        for (String prePrePos : prePrePosSet) {
                                                System.out.print("\tdb 60: " + prePrePos + " " + prePos + " " + curPos);
                                                double oldVal = dp.get(i).get(prePos).get(curPos);
                                                double prev = 0;
                                                if(dp.get(i-1).containsKey(prePrePos) && dp.get(i-1).get(prePrePos).containsKey(prePos))
                                                        prev = (dp.get(i - 1).get(prePrePos).get(prePos));
                                                else {
                                                        System.out.println("66: prePrePos = " + prePrePos +" prePos = " + prePos +" curPos = " + curPos );
                                                }
                                                double tran = 0;
                                                if(trans.containsKey(prePrePos+"_"+prePos) && trans.get(prePrePos+"_"+prePos).containsKey(curPos))
                                                        tran = trans.get(prePrePos + "_" + prePos).get(curPos);
                                                else {
                                                        //System.out.println("HAHAHAHAHAHAHAHAHHAAHAHDEBUG 72: prePrePos = " + prePrePos +" prePos = " + prePos +" curPos = " + curPos );
                                                }
                                                double emit = emits.get(curPos).get(s.get(i));
                                                double newVal = prev * tran * emit;
                                                System.out.println(" = "+ newVal*Math.pow(10, 13));
//                                                if(newVal == 0.0)
//                                                        System.out.println("78: " + i);



                                                if (newVal > oldVal) {
                                                        dp.get(i).get(prePos).put(curPos, newVal);
                                                        bp.get(i).get(prePos).put(curPos, prePrePos);

                                                        maxRec = newVal * Math.pow(10, 13);
                                                }
                                                else {
                                                        //System.out.println(newVal + "<=" + oldVal);
                                                }
                                        }
                                }
                        }
                        System.out.println(" max = " + maxRec);
                }

//                for(int i=2; i<s.size(); i++) {
//                        System.out.println("DEBUG 84: i = " + i + ": " + dp.get(i));
//                }

                double curVal = 0.0;
                String prePos = "", curPos = "";
                for(String pre: wordTag.get(s.get(s.size()-2))) {
                        for(String cur: wordTag.get(s.get(s.size()-1))) {
                                double preVal = dp.get(s.size()-1).get(pre).get(cur);
                                double tran = trans.get(pre+"_"+cur).get(EDPOS);
                                double newVal = preVal * tran;
                                if( curVal < newVal ) {
                                        curVal = newVal;
                                        prePos = pre;
                                        curPos = cur;
                                }
                        }
                }

                Stack<String> pos_stack = new Stack<>();
                pos_stack.push(curPos);
                pos_stack.push(prePos);
                for(int i = s.size()-3; i>=0; i--) {
                        pos_stack.push(bp.get(i+2).get(prePos).get(curPos));
                        curPos = prePos;
                        prePos = pos_stack.peek();
                }

                for(int i=0; i<s.size(); i++) {
                        try {
                                bw.write(s.get(i)+"\t"+pos_stack.pop());
                                bw.newLine();
                        } catch (IOException e) {
                                e.printStackTrace();
                        }
                }
        }

        public static void main(String[] args) {

                if (args.length == 0) {
                        System.err.println("ERROR! Format: java Tagger trainCorpus.pos test.words");
                        System.exit(1);
                }

                try {
                        // read in the training corpus file
                        Scanner scan = new Scanner(new File(args[0]));

                        // KEY: previous two states(linked by '_'); VALUE: cur State and count from previous two states
                        HashMap<String, HashMap<String, Double>> trans = new HashMap<>();

                        // KEY: state;  VALUE: word count under that state
                        HashMap<String, HashMap<String, Double>> emit = new HashMap<>();

                        // create a map to store all words and set of states for each word
                        // KEY: word; VALUE: a set of states for that word
                        HashMap<String, HashSet<String>> wordTag = new HashMap<>();
                        wordTag.put(STTOKEN, new HashSet<String>()); wordTag.get(STTOKEN).add(STPOS);
                        wordTag.put(EDTOKEN, new HashSet<String>()); wordTag.get(EDTOKEN).add(EDPOS);

                        // read in training corpus
                        String prePrePos = STPOS, prePos = STPOS;
                        while (scan.hasNextLine()) {
                                // read one sentence, process that sentence, and continue reading next sentence
                                String line = scan.nextLine().trim();

                                if (line.isEmpty() && (prePos!=STPOS || prePrePos!=STPOS) ) {
                                        String pos = EDPOS;
                                        // only handles "pos transition count"
                                        String tmpPos = prePrePos + "_" + prePos;
                                        if( !trans.containsKey(tmpPos) )
                                                trans.put(tmpPos, new HashMap<String, Double>());
                                        trans.get(tmpPos).put(pos, (trans.get(tmpPos).containsKey(pos)? trans.get(tmpPos).get(pos)+1: 1.0));
                                        prePrePos = STPOS;
                                        prePos = STPOS;
                                }

                                else if(!line.isEmpty()){
                                        String[] tmp = line.split("\\s+");
                                        String token = tmp[0], pos = tmp[1];

                                        // handles "pos transition count"
                                        String tmpPos = prePrePos + "_" + prePos;
                                        if( !trans.containsKey(tmpPos) )
                                                trans.put(tmpPos, new HashMap<String, Double>());
                                        trans.get(tmpPos).put(pos, (trans.get(tmpPos).containsKey(pos)? trans.get(tmpPos).get(pos)+1: 1.0));
                                        prePrePos = prePos;
                                        prePos = pos;

                                        // handles "emit count"
                                        if( !emit.containsKey(pos))
                                                emit.put ( pos, new HashMap<String, Double>());
                                        emit.get(pos).put(token, (emit.get(pos).containsKey(token)? emit.get(pos).get(token)+1: 1.0));

                                        // add the <word_tag> pair into "wordTag"
                                        if(!wordTag.containsKey(token))
                                                wordTag.put(token, new HashSet<String>());
                                        wordTag.get(token).add(pos);
                                }
                        }
                        scan.close();

                        // read in first time to find out all unknown words
                        Scanner scan2 = new Scanner(new File(args[1]));
                        while (scan2.hasNextLine()) {
                                String word = scan2.nextLine().trim();
                                if (!wordTag.containsKey(word)) {
                                        // word with "-" are more likely to be ADJ
                                        if( word.contains("-") ) {
                                                if(!emit.containsKey("JJ"))
                                                        emit.put("JJ", new HashMap<String, Double>());
                                                emit.get("JJ").put(word, emit.get("JJ").containsKey(word) ? emit.get("JJ").get(word) + 1 : 1.0);
                                                wordTag.put(word, new HashSet<>()); wordTag.get(word).add("JJ");
                                        }
                                        else if( (!word.isEmpty()) && Character.isUpperCase(word.charAt(0)) ) {
                                                if(word.endsWith("s")) {
                                                        if (!emit.containsKey("NNPS"))
                                                                emit.put("NNPS", new HashMap<String, Double>());
                                                        emit.get("NNPS").put(word, emit.get("NNPS").containsKey(word) ? emit.get("NNPS").get(word) + 1 : 1.0);
                                                        wordTag.put(word, new HashSet<>()); wordTag.get(word).add("NNPS");
                                                }
                                                else {
                                                        if (!emit.containsKey("NNP"))
                                                                emit.put("NNP", new HashMap<String, Double>());
                                                        emit.get("NNP").put(word, emit.get("NNP").containsKey(word) ? emit.get("NNP").get(word) + 1 : 1.0);
                                                        wordTag.put(word, new HashSet<>()); wordTag.get(word).add("NNP");
                                                }
                                        }
                                        else if( (!word.isEmpty()) && !Character.isUpperCase(word.charAt(0)) && word.endsWith("s") ) {
                                                if (!emit.containsKey("NNS"))
                                                        emit.put("NNS", new HashMap<String, Double>());
                                                emit.get("NNS").put(word, emit.get("NNS").containsKey(word) ? emit.get("NNS").get(word) + 1 : 1.0);
                                                wordTag.put(word, new HashSet<>()); wordTag.get(word).add("NNS");
                                        }
                                        else if( (!word.endsWith("ed")) ) {
                                                if (!emit.containsKey("VBN"))
                                                        emit.put("VBN", new HashMap<String, Double>());
                                                emit.get("VBN").put(word, emit.get("VBN").containsKey(word) ? emit.get("VBN").get(word) + 1 : 1.0);
                                                wordTag.put(word, new HashSet<>()); wordTag.get(word).add("VBN");
                                        }
                                        else if( word.endsWith("ing")) {
                                                if (!emit.containsKey("VBG"))
                                                        emit.put("VBG", new HashMap<String, Double>());
                                                emit.get("VBG").put(word, emit.get("VBG").containsKey(word) ? emit.get("VBG").get(word) + 1 : 1.0);
                                                wordTag.put(word, new HashSet<>()); wordTag.get(word).add("VBG");
                                        }
                                        else if (  word.endsWith("able")  || word.endsWith("ive")  || word.endsWith("al") || word.endsWith("ous") ) {
                                                if (!emit.containsKey("JJ"))
                                                        emit.put("JJ", new HashMap<String, Double>());
                                                emit.get("JJ").put(word, emit.get("JJ").containsKey(word) ? emit.get("JJ").get(word) + 1 : 1.0);
                                                wordTag.put(word, new HashSet<>()); wordTag.get(word).add("JJ");
                                        }
                                        // others are unknown, assume they appear in every POS with equal probabilities
                                        else {
                                                for(HashMap.Entry<String, HashMap<String, Double>> entry: emit.entrySet()) {
                                                        entry.getValue().put(word, 1.0 /emit.size());
                                                        wordTag.put(word, new HashSet<>());
                                                        // add all possible poses to this unknown word
                                                        for(HashMap.Entry<String, HashMap<String, Double>> e: emit.entrySet()) {
                                                                wordTag.get(word).add(e.getKey());
                                                        }
                                                }
                                        }
                                }
                        }
                        scan2.close();

                        // translate count into probability
                        for (HashMap.Entry<String, HashMap<String, Double>> entry : trans.entrySet()) {
                                double sum = 0.0;
                                for (HashMap.Entry<String, Double> e : entry.getValue().entrySet())
                                        sum += e.getValue();
                                for (HashMap.Entry<String, Double> e : entry.getValue().entrySet()) {
                                        if (e.getValue() >= 1) {
                                                e.setValue(e.getValue() / sum);
                                                //System.out.println("DB 255: P("+entry.getKey()+" => "+e.getKey()+") =" +e.getValue());
                                        }
                                }
                        }

                        for (HashMap.Entry<String, HashMap<String, Double>> entry : emit.entrySet()) {
                                double sum = 0.0;
                                for (HashMap.Entry<String, Double> e : entry.getValue().entrySet())
                                        sum += e.getValue();
                                for (HashMap.Entry<String, Double> e : entry.getValue().entrySet())
                                        if (e.getValue() >= 1) {
                                                e.setValue(e.getValue() / sum);
                                                //System.out.println("DB 275: P(" + entry.getKey() + " | " + e.getKey() + ") = " + e.getValue());
                                        }
                        }
                        emit.put(STPOS, new HashMap<String, Double>()); emit.get(STPOS).put(STTOKEN, 1.0);
                        emit.put(EDPOS, new HashMap<String, Double>()); emit.get(EDPOS).put(EDTOKEN, 1.0);


                        System.out.println("292: exist or not: " + trans.get("NN" + "_" + "VB"));

                        // second read
                        scan = new Scanner(new File(args[1]));
                        ArrayList<String> s = new ArrayList<>();
                        s.add(STTOKEN); s.add(STTOKEN);
                        BufferedWriter bw = new BufferedWriter(new FileWriter("my.pos"));

                        while (scan.hasNextLine()) {
                                // read one sentence, process that sentence, and continue reading next sentence until eof
                                String word = scan.nextLine().trim();

                                if (word.isEmpty()) {
                                        viterbi(s, bw, trans, emit, wordTag);

                                        s = new ArrayList<String>();
                                        s.add(STTOKEN); s.add(STTOKEN);
                                }
                                else {
                                        s.add(word);
                                }
                        }
                        bw.close();
                        scan.close();

                } catch (FileNotFoundException e) {
                        System.err.println("ERROR! Selected File Not Found.");
                } catch (IOException e) {
                        System.err.println("ERROR! Cannot write to this file");
                }
        }
}