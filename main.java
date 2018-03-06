import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;

import java.io.*;
import java.util.*;
import java.lang.reflect.Method;
import java.util.concurrent.locks.ReentrantLock;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.*;

public class main extends Application {

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Assignment 1");
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setInitialDirectory(new File("."));
        File directory = directoryChooser.showDialog(primaryStage); 
        
//        thread.init();
//        thread tc1 = new thread("thread 1");
//        Thread t1 = new Thread(tc1);
//        thread tc2 = new thread("thread 2");
//        Thread t2 = new Thread(tc2);
//        t1.start();
//        t2.start();
//        System.out.println("started");
//        System.out.println(thread.threads.size());
//        threadScheduler ts = new threadScheduler();
//        //threadScheduler.noCurrentTask = false;
//        new Thread(ts).start();
//        ts.updateAvail();
        
        words.init(directory);
        threadScheduler.start(2);
        threadScheduler.assignTask(words.processFiles, words.trainFolder, words.readFile);
        
        
        //System.out.println("waiting for tasks to finish");
        while (!threadScheduler.noCurrentTask) {
            Thread.currentThread().yield();
        }
        //System.out.println("tasks completed");
//        threadScheduler.kill = true;
//        while (!threadScheduler.dead) {
//            Thread.currentThread().yield();
//        }
//        for (Object o : words.trainSpamFreq.keySet().toArray()) {
//            System.out.println((String)o);
//        }
        words.genProbs();
        threadScheduler.assignTask(words.processFiles, words.testFolder, words.examineFile);
        while (!threadScheduler.noCurrentTask) {
            Thread.currentThread().yield();
        }
        //System.out.println("generatedProbs");
//        System.out.println(words.generatedProbs.entrySet());
//        for (TestFile t : words.testFileList) {
//            System.out.println(t.getFilename() + " " + t.getSpamProbRounded());
//        }
        threadScheduler.kill = true;
        while (!threadScheduler.dead) {
            Thread.currentThread().yield();
        }
        words.calcAccAndPrec();
//        Enumeration s = words.hamWords.keys();
//        while(s.hasMoreElements()) {
//            System.out.println(s.nextElement());
//        }
        //System.exit(0);
        
        TableView<TestFile> table = new TableView<>();
        table.setItems(FXCollections.observableArrayList(words.testFileList));
        //table.setEditable(true);
        TableColumn<TestFile, String> fileNameColumn = new TableColumn<>("File Name");
        fileNameColumn.setMinWidth(200);
        fileNameColumn.setCellValueFactory(new PropertyValueFactory<>("filename"));

        TableColumn<TestFile, String> classColumn = new TableColumn<>("Actual Class");
        classColumn.setMinWidth(100);
        classColumn.setCellValueFactory(new PropertyValueFactory<>("actualClass"));

        TableColumn<TestFile, Double> spamProbColumn = new TableColumn<>("Spam Probability");
        spamProbColumn.setMinWidth(200);
        spamProbColumn.setCellValueFactory(new PropertyValueFactory<>("spamProbability"));

        table.getColumns().add(fileNameColumn);
        table.getColumns().add(classColumn);
        table.getColumns().add(spamProbColumn);
        
        Label accuracyLabel = new Label();
        accuracyLabel.setText("Accuracy");
        
        TextField accuracyField = new TextField();
        accuracyField.setText(String.valueOf(words.accuracy));
        
        Label precisionLabel = new Label();
        precisionLabel.setText("Precision");
        
        TextField precisionField = new TextField();
        if (words.precision < 0) {
            precisionField.setText("All files categorized as ham");
        }
        else {
            precisionField.setText(String.valueOf(words.precision));
        }
        
        GridPane gridPane = new GridPane();
        gridPane.add(accuracyLabel, 0, 0);
        gridPane.add(accuracyField, 1, 0);
        gridPane.add(precisionLabel, 0, 1);
        gridPane.add(precisionField, 1, 1);
        

        BorderPane layout = new BorderPane();
        layout.setCenter(table);
        layout.setBottom(gridPane);

        primaryStage.setScene(new Scene(layout, 700, 500));
        primaryStage.show();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

}

class words {
    static int x = 4;
    static File dir;
    static File trainFolder;
    static File testFolder;
    static ConcurrentHashMap<String, AtomicInteger> trainSpamFreq = new ConcurrentHashMap();
    static ConcurrentHashMap<String, AtomicInteger> trainHamFreq = new ConcurrentHashMap();
    static Method processFiles;
    static Method readFile;
    static Method examineFile;
    static TreeMap<String, Double> generatedProbs = new TreeMap<>();
    static ArrayList<TestFile> testFileList = new ArrayList<>();
    static ReentrantLock l = new ReentrantLock();
    static double accuracy;
    static double precision;

    
    static void init(File file) {
        try {
            processFiles = words.class.getDeclaredMethod("processFiles", File.class, Method.class);
            readFile = words.class.getDeclaredMethod("readFile", File.class);
            examineFile = words.class.getDeclaredMethod("examineFile", File.class);
        }
        catch (Exception e) {
            System.out.println("thread init Exception");
        }
        
        dir = file;
        trainFolder = new File(file.getPath() + "\\train");
        testFolder = new File(file.getPath() + "\\test");
    }
    
    public static void processFiles(File file, Method m) throws IOException {
        //System.out.println("Processing " + file.getAbsolutePath() + "...");
        //System.out.println("thread: " + Thread.currentThread().getName() + " | Processing " + file.getName());
        if (file.isDirectory()) {
            //System.out.println("is directory");
            // process all the files in that directory
            File[] contents = file.listFiles();
            for (File current : contents) {
                threadScheduler.assignTask(processFiles, current, m);
            }
        } else if (file.exists()) {
            // count the words in this file
            //System.out.println("is file");
            threadScheduler.assignTask(m, file);
        }
        else {
            System.out.println("File not valid");
        }
        //System.out.println("thread: " + Thread.currentThread().getName() + " | finished processing");
    }
    
    static void readFile(File file) {
        ConcurrentMap<String, AtomicInteger> dataSet = (file.getParentFile().getName().equals("spam")) ? trainSpamFreq : trainHamFreq;
        ArrayList<String> fileWords = new ArrayList<>();
        try {
            Scanner scanner = new Scanner(file);
            //scanner.useDelimiter("\\s");//"[\s\.;:\?\!,]");//" \t\n.;,!?-/\\");
            while (scanner.hasNext()) {
                String word = scanner.next();
                //System.out.println(word);
                if (!fileWords.contains(word)) {
                    fileWords.add(word);
                }
            }
            for (String word : fileWords) {
                if (!dataSet.containsKey(word)) {
                    dataSet.putIfAbsent(word, new AtomicInteger(0));
                }
                dataSet.get(word).getAndIncrement();
                
            }
            scanner.close();
        }
        catch (FileNotFoundException e) {
            System.out.println("thread readFile exception");
        }
        //System.out.println("thread: " + Thread.currentThread().getName() + " | finished reading file");
    }
    
    static void examineFile(File file) {
        ArrayList<String> fileWords = new ArrayList<>();
        try {
            Scanner scanner = new Scanner(file);
            //scanner.useDelimiter("\\s\\n\\.");//"[\s\.;:\?\!,]");//" \t\n.;,!?-/\\");
            while (scanner.hasNext()) {
                String word = scanner.next();
                //System.out.println(word);
                if (!fileWords.contains(word)) {
                    fileWords.add(word);
                }
            }
            scanner.close();
            double n = 0.0;
            for (String word : fileWords) {
                if (generatedProbs.containsKey(word)) {
                    double PrSW = generatedProbs.get(word);
                    if (PrSW == 0) {
                        int x = 2;
                    }
                    n += Math.log(1 - PrSW) - Math.log(PrSW);
                }
                
            }
            double PrSF = 1.0 / (1.0 + Math.pow(Math.E, n));

            TestFile test = new TestFile(file.getName(), PrSF, file.getParentFile().getName());
            l.lock();
            testFileList.add(test);
            l.unlock();
        }
        catch (FileNotFoundException e) {
            System.out.println("thread readFile exception");
        }
    }
    
    static void genProbs() {
        double spamFiles = new File(trainFolder.getPath() + "\\spam").listFiles().length;
        double hamFiles = new File(trainFolder.getPath() + "\\ham").listFiles().length +
                new File(trainFolder.getPath() + "\\ham2").listFiles().length;
        for (Object w : trainSpamFreq.keySet().toArray()) {
            String word = (String)w;
            //System.out.println("[ " + word + " ]");
            double prWS = (double)trainSpamFreq.get(word).get() / spamFiles;
            double prWH = 0;
            if (trainHamFreq.containsKey(word)) {
                prWH = (double)trainHamFreq.get(word).get() / hamFiles;
            }
            generatedProbs.put(word, prWS / (prWS + prWH));
        }
    }
    
    static void calcAccAndPrec() {
        int truePositives = 0;
        int trueNegatives = 0;
        int falsePositives = 0;
        
        for (TestFile test : testFileList) {
            if (test.getActualClass().equals("spam") && (test.getSpamProbability() >= 0.5)) {
                truePositives++;
            }
            else if (test.getActualClass().equals("ham") && (test.getSpamProbability() < 0.5)) {
                trueNegatives++;
            }
            else if (test.getActualClass().equals("ham") && (test.getSpamProbability() >= 0.5)) {
                falsePositives++;
            }
        }
        accuracy = (truePositives + trueNegatives) / testFileList.size();
        try {
            precision = truePositives / (falsePositives + truePositives);
        }
        catch (ArithmeticException e) {
            precision = -1;
        }
    }
}

class thread implements Runnable {
    volatile boolean available;
    volatile boolean kill;
    volatile static ArrayList<thread> threads = new ArrayList<>();
    volatile Method funcArg;
    //int funcArgSet = 0;
    //volatile File fileArg;
    Object[] args;
    volatile boolean indexed;
    //static Method processFiles;
    //static Method readFile;
    String name;
    
    thread(String n) {
        available = true;
        kill = false;
        threads.add(this);
        threadScheduler.availNumber.getAndIncrement();
        name = n;
    }
    
    thread() {
        available = true;
        kill = false;
        threads.add(this);
        threadScheduler.availNumber.getAndIncrement();
        
    }

    public void run() {
        //name = Thread.currentThread().getName();
        //while (!kill || (funcArg != null)) { //exit if kill is true and funcArg is null
        while (!kill) {  
            //System.out.println("thread: " + name + " | run while");
            if (funcArg != null) {
                //System.out.println("thread run name: " + name);
                //available = false;
                //indexed = false;
                
                try {
                    //funcArg.invoke(this, fileArg);
                    //System.out.println("function: " + funcArg.getName());
//                    for (int i = 0; i < args.length; i++) {
//                        System.out.println("arg " + i + " type: " + args[i].getClass());
//                        System.out.println("arg " + i + " value: " + args[i]);
//                    }
                    funcArg.invoke(null, args);
                    //System.out.println("thread: " + name + " | finished function");
                }
                catch (Exception e) {
                    System.out.println("thread run funcArg Exception");
                    
                    e.printStackTrace();
                }
                
                funcArg = null;
                //funcArgSet--;
                //fileArg = null;
                args = null;
                available = true;
                threadScheduler.availNumber.getAndIncrement();
                //System.out.println("increased threadScheduler.availNumber to " + threadScheduler.availNumber);
                //threadScheduler.updateAvail();
                //System.out.println("Avail updated");
            }
            else {
                Thread.currentThread().yield();
            }
//            if (funcArgSet != 0) {
//                System.out.println("thread: " + name + " | funcArgSet is not 0");
//            }
        }
        //System.out.println("thread: " + name + " | exiting run");
    }
    
//    public void printFunc() {
//        if (funcArg == null) {
//            System.out.println("thread: " + name + " | funcArg check null");
//        }
//        else {
//            System.out.println("thread: " + name + " | funcArg check " + funcArg.getName());
//        }
//    }
}

//class threadScheduler implements Runnable {
class threadScheduler extends Thread {
    private static volatile thread[] availThreads = new thread[0];
    static AtomicInteger availIndex = new AtomicInteger(0);
    static AtomicInteger availNumber = new AtomicInteger(0);
    static boolean kill = false;
    static ReentrantLock l = new ReentrantLock();
    static boolean noCurrentTask = true;
    static volatile boolean dead = false;
    
    public static void start(int n) {
        //thread.init();
        while (n > 0) {
            new Thread(new thread(), String.valueOf(n)).start();
            n--;
        }
        //new threadScheduler().start();
        threadScheduler.updateAvail();
        new threadScheduler().start();
    }
    
    public void run() {
        while (!kill) {
            //System.out.print("");
            //System.err.println("availNumber: " + availNumber);
            //if ((availNumber > 2) && (availThreads.length - 1 - availIndex < 2) && (getCalls == 0) || (availThreads.length - 1 - availIndex == 0)) {
            if (availNumber.get() > 0) {
                //System.out.println("                                                                updating avail");
                updateAvail();
            }
            //none of the threads are processing anything if all the threads are
            //in the array of available threads and the next available thread is
            //the first thread in the array of available threads
            if ((availThreads.length == thread.threads.size()) && (availIndex.get() == 0)) {
                //System.out.println("threadScheduler noCurrentTask");
                noCurrentTask = true;
            }
            else {
                noCurrentTask = false;
            }
        }
        //System.out.println("ThreadScheduler terminating");
        for (thread t : thread.threads) {
            t.kill = true;
        }
        dead = true;
    }
    
    static void updateAvail() {
        //System.out.println("updateAvail");
        //thread[] temp = new thread[availNumber + availThreads.length - availIndex];
        int tempIndex = availIndex.get();
        //int tempNumber = availNumber;
        thread[] temp = new thread[availNumber.get() + availThreads.length - tempIndex];
        int i = 0;
        while (availThreads.length > tempIndex + i) {
            temp[i] = availThreads[tempIndex + i];
            i++;
        }
        for (thread t : thread.threads) {
            if (t.available && !t.indexed && (i < temp.length)) {
                t.indexed = true;
                temp[i] = t;
                i++;
                availNumber.getAndDecrement();
            }
        }
        
        l.lock();
        //System.out.println("update locked");
        //availIndex = availIndex - tempIndex;
        availIndex.getAndAdd(-tempIndex);
//        if (availIndex.get() < 0) {
//            int x = 0;
//        }
        availThreads = temp;
        //System.out.println("update unlocking");
        //System.out.println("availIndex: " + availIndex.get());
        //System.out.println("availThreads.length: " + availThreads.length);
        
        l.unlock();
        
    }
    
    static thread getThread() {
        thread temp = null;
        l.lock();
        if (availThreads.length > availIndex.get()) {
            availIndex.getAndIncrement();
            temp = availThreads[availIndex.get() - 1];
            temp.available = false;
            temp.indexed = false;
        }
        l.unlock();
        
        return temp;
    }
    
    /*
    static void assignTask(Method method, File file) {
        noCurrentTask = false;
        thread t = getThread();
        t.funcArg = method;
        t.fileArg = file;
        //t.funcArgSet++;
    }
    */
    
    static void assignTask(Method method, Object... args) {
        noCurrentTask = false;
        thread t = getThread();
        if (t != null) {
            //System.out.println("new thread");
            t.args = args;
            t.funcArg = method;
        }
        else {
            try {
                method.invoke(null, args);
            }
            catch (Exception e) {
                System.out.println("threadScheduler assignTask invoke exception");
            }
        }
        //t.funcArg = method;
        //t.args = args;
    }
}
/*
class dataMap<K extends String, V extends AtomicInteger> implements ConcurrentMap {
    List keys = new ArrayList();
    List values = new ArrayList();
    
    dataMap() {
        keys = Collections.synchronizedList(keys);
        values = Collections.synchronizedList(values);
    }
            
    V replace(K key, V value) {
        if (keys.contains(key)) {
            values
        }
    }
}
*/
