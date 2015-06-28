/**
 *  
 *	Fachhochschule Münster - Fachbereich Elektrotechnik und Informatik
 * 		Labor für IT-Sicherheit		www.its.fh-muenster.de
 *	
 *	Copyright (C) 2014  Dennis Loehr
 *   
 *	This program is free software; you can redistribute it and/or modify it 
 *		under the terms of the GNU General Public License as published by the 
 *		Free Software Foundation; either version 3 of the License, or (at 
 *		your option) any later version.
 *
 *	This program is distributed in the hope that it will be useful, but 
 *		WITHOUT ANY WARRANTY; without even the implied warranty of 
 *		MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 *		General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along 
 *  	with this program; if not, see <http://www.gnu.org/licenses/>.
 *  
 */

package de.fhmuenster.its.plag;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Semaphore;

/**
 * Class to check a path for duplicate text entries
 *
 * @author loehr
 */
public class Main {

    /**
     * Path to check
     */
    public final static String path = "/home/loehr/Dokumente/Praktika PSE/";
    /**
     * File extensions that have to be checked
     */
    final static String[] extensions = {"c", "h"};
    /**
     * Blacklist of files that have to be excluded
     */
    final static String[] blacklist = {"base64.c", "echo_server.c"};

    /**
     * minimal count of matching token in a row
     */
    public final static int minTokenMatch = 6;
    /**
     * minimum similarity
     *
     * @deprecated
     */
    public final static float minSimilarity = 0.5f;

    private static final StringBuilder sbSimilarity = new StringBuilder();
    private static final StringBuilder sbCountTitles = new StringBuilder();

    /**
     * function to append a string to both csv files
     *
     * @param s String that have to be appended
     */
    public static void append(String s) {
        sbSimilarity.append(s);
        sbCountTitles.append(s);
    }

    /**
     * Class to store the multithreading job informations
     */
    public static class Job {

        /**
         * Name of the first content String (Dirname)
         */
        //public String dirName1;
        /**
         * Name of the second content String (Dirname)
         */
        //public String dirName2;

        /**
         * Content of the first directory
         */
        public String content1;
        /**
         * Content of the second directory
         */
        public String content2;
        
        /**
         * result of the similarity 
         */
        public float similarity;
        /**
         * named title that match with an other 
         */
        public ArrayList<MatchVals> tiles = new ArrayList<>();
        /**
         * variable to store the status of the job
         */
        public boolean jobStarted = false;

    }

    /**
     * Worker for the multithreading
     */
    public static class Worker implements Runnable {

        private List<Job> jobList;
        private Semaphore mutex;
        private Thread t;

        /**
         * Constructor to create the worker
         * @param pendingJobs
         * @param mutex 
         */
        public Worker(List<Job> pendingJobs, Semaphore mutex) {
            init(pendingJobs, mutex);
        }

        /**
         * init of the worker 
         * @param pendingJobs
         * @param mutex 
         */
        public final void init(List<Job> pendingJobs, Semaphore mutex) {
            this.jobList = pendingJobs;
            this.mutex = mutex;
            t = new Thread(this);
        }

        /**
         * start the worker
         */
        public void start() {
            t.start();
        }

        /**
         * wait for the end of all jobs 
         * @throws InterruptedException 
         */
        public void join() throws InterruptedException {
            t.join();
        }

        /**
         * The calculation function of the worker
         */
        @Override
        public void run() {
            try {
                while (jobList.size() > 0) {
                    Job myJob = null;
                    mutex.acquire();
                    for (int i = 0; i < jobList.size(); i++) {
                        if (!jobList.get(i).jobStarted) {
                            myJob = jobList.get(i);
                            myJob.jobStarted = true;
                            System.out.println(String.format("Job %d out of %d started ", i, jobList.size()));
                            break;
                        }
                    }
                    mutex.release();
                    if (myJob == null) {
                        break;
                    }

                    String s1 = myJob.content1;
                    List<String> s1list = createTokenListWithoutEmptyElements(s1);
                    List<String> s2list = createTokenListWithoutEmptyElements(myJob.content2);
                    ArrayList<MatchVals> tiles;
                    tiles = GreedyStringTiling.RKR_GST(
                            s1list.toArray(new String[s1list.size()]),
                            s2list.toArray(new String[s2list.size()]), minTokenMatch, 20);
                    SimVal simResult = SimilarityCalculator.calcSimilarity(
                            s1list, s2list, tiles, minSimilarity);
                    float similarity = simResult.similarity;
                    myJob.similarity = similarity;
                    myJob.tiles = tiles;

                }
            } catch (InterruptedException e) {
            }
        }
    }

    
    /**
     * Main program for the the plagiate checker 
     * @param args
     * @throws InterruptedException 
     */
    public static void main(String[] args) throws InterruptedException {
        ArrayList<String> dirnames = new ArrayList<>();
        ArrayList<String> strings = new ArrayList<>();
        fillInDirectorys(dirnames, strings);
        append("Filename;");
        for (int i = 0; i < dirnames.size(); i++) {
            append(dirnames.get(i));
            append(";");
        }
        append("\n");

        List<Job> jobList = new ArrayList<>();
        for (int i = 0; i < dirnames.size(); i++) {
            for (int k = 0; k < dirnames.size(); k++) {
                Job job = new Job();
                job.content1 = strings.get(i);
                //job.dirName1 = dirnames.get(i);
                job.content2 = strings.get(k);
                //job.dirName2 = dirnames.get(k);
                jobList.add(job);
            }
        }
        Worker[] workerlist = new Worker[Runtime.getRuntime().availableProcessors() + 1];
        Semaphore mutex = new Semaphore(1);
        for (int i = 0; i < workerlist.length; i++) {
            workerlist[i] = new Worker(jobList, mutex);
            workerlist[i].start();
        }
        for (Worker w : workerlist) {
            w.join();
        }

        for (int i = 0; i < dirnames.size(); i++) {
            append(dirnames.get(i));
            append(";");

            String s1 = strings.get(i);
            List<String> s1list = createTokenListWithoutEmptyElements(s1);

            boolean[] s1dir = new boolean[s1list.size()];
            for (int g = 0; g < s1dir.length; g++) {
                s1dir[g] = false;
            }
            for (int k = 0; k < dirnames.size(); k++) {
                if (i != k) {
                    Job j = jobList.get((i * dirnames.size()) + k);

                    // FIXME add done jobs
                    setTokenWithPlagiate(s1dir, j.tiles);
                    sbSimilarity.append(String.format("%04f", j.similarity));
                    System.out.println(dirnames.get(i) + ":" + dirnames.get(k)
                            + "= " + String.format("%04f", j.similarity));
                    sbCountTitles.append(j.tiles.size());
                }
                append(";");
            }
            StringBuilder sbHTML;
            sbHTML = createPlagIndicationHTML(s1, s1list, s1dir);
            writeFileFromString("Plag-" + dirnames.get(i) + "-" + minTokenMatch
                    + ".html", sbHTML.toString());
            append("\n");
        }
        writeFileFromString("Plag-Similarity-" + minTokenMatch + ".csv",
                sbSimilarity.toString());
        writeFileFromString("Plag-Titles-" + minTokenMatch + ".csv",
                sbCountTitles.toString());
    }

    private static void writeFileFromString(String filename, String HTML) {
        FileWriter fw = null;

        try {
            fw = new FileWriter(filename);
            fw.write(HTML);
            fw.flush();
            fw.close();
        } catch (IOException e) {
            try {
                if (fw != null) {
                    fw.close();
                }
            } catch (IOException e1) {
            }
        }
    }

    private static StringBuilder createPlagIndicationHTML(String s1,
            List<String> s1list, boolean[] s1dir) {
        StringBuilder sbHTML;
        sbHTML = new StringBuilder();
        sbHTML.append("<html>\n");
        sbHTML.append("<pre> <code>\n");
        boolean lastState = false;
        int lastPos = 0;
        for (int g = 0; g < s1dir.length; g++) {
            String p = s1list.get(g);
            if (!p.isEmpty()) {
                if (lastState != s1dir[g]) {
                    if (!lastState) {
                        sbHTML.append("<span style='background-color: highlight;'>");
                    } else {
                        sbHTML.append("</span>");
                    }
                    lastState = s1dir[g];
                }

                // text hinzufügen
                int newPos = s1.indexOf(p, lastPos);
                String esc = s1.substring(lastPos, newPos);
                esc = esc.replaceAll("<", "&lt;");
                sbHTML.append(esc);
                lastPos = newPos;
            }
        }
        String esc = s1.substring(lastPos, s1.length());
        esc = esc.replaceAll("<", "&lt;");
        sbHTML.append(esc);

        sbHTML.append("</code> </pre>\n");
        sbHTML.append("</html>\n");
        return sbHTML;
    }

    private static void setTokenWithPlagiate(boolean[] s1dir,
            ArrayList<MatchVals> tiles) {
        for (int g = 0; g < tiles.size(); g++) {
            MatchVals ma = tiles.get(g);
            for (int h = ma.patternPostion; h < ma.patternPostion + ma.length; h++) {
                s1dir[h] = true;
            }
        }
    }

    private static List<String> createTokenListWithoutEmptyElements(String s1) {
        List<String> s1list;
        s1list = new ArrayList<>();
        s1list.addAll(Arrays.asList(s1.split("[\\s+|\\W+]")));

        for (int x = s1list.size() - 1; x >= 0; x--) {
            if (s1list.get(x).isEmpty()) {
                s1list.remove(x);
            }
        }
        return s1list;
    }

    private static void fillInDirectorys(ArrayList<String> dirnames,
            ArrayList<String> strings) {
        File f = new File(path);
        ArrayList<String> sortList = new ArrayList<>();
        for (File lf : f.listFiles()) {
            sortList.add(lf.getAbsolutePath());
        }
        Collections.sort(sortList);
        for (String sf : sortList) {
            File s = new File(sf);
            if (s.isDirectory()) {
                String tmp = getStringFromDir(s.getAbsolutePath(), extensions,
                        blacklist);
                if (tmp.length() < 1000) {
                    continue;
                }
                System.out.println("Dirname: " + s.getName());
                dirnames.add(s.getName());
                System.out.println("length: " + tmp.length() + "\n");
                strings.add(tmp);
            }
        }
    }

    /**
     * read in all files that have the extension and are not on the blacklist
     * @param baseDir
     * @param fileExtensions
     * @param fileBlacklist
     * @return 
     */
    public static String getStringFromDir(String baseDir,
            String[] fileExtensions, String[] fileBlacklist) {
        StringBuilder sb = new StringBuilder();
        File f = new File(baseDir);
        if (f.isDirectory()) {
            File[] sub = f.listFiles();
            for (File t : sub) {
                if (t.isFile()) {
                    String tmpFilename = t.getName();

                    String[] b = tmpFilename.split("\\.");
                    if (b.length > 0) {
                        for (String ext : fileExtensions) {
                            if (ext.equalsIgnoreCase(b[b.length - 1])) {
                                boolean cont = false;
                                for (String blacklist : fileBlacklist) {
                                    if (blacklist.equals(tmpFilename)) {
                                        // System.out.println("continue: "+tmpFilename);
                                        cont = true;
                                        break;
                                    } else {
                                        // System.out.println("\""+tmpFilename+"\" != \""+blacklist+"\"");
                                    }
                                }
                                if (cont) {
                                    break;
                                }
                                try {
                                    String tmpFileString = readFile(t);

                                    sb.append("\n");
                                    sb.append("//");
                                    sb.append(tmpFilename);
                                    sb.append("\n");
                                    sb.append(tmpFileString);
                                    sb.append("\n");
                                } catch (IOException e) {
                                }
                            }
                        }
                    }
                } else if (t.isDirectory()) {
                    sb.append(getStringFromDir(t.getAbsolutePath(),
                            fileExtensions, fileBlacklist));
                }
            }
        }
        return sb.toString();
    }

    /**
     * read in one file
     * @param file
     * @return
     * @throws IOException 
     */
    public static String readFile(File file) throws IOException {
        byte[] enc = Files.readAllBytes(Paths.get(file.getAbsolutePath()));
        String ret = new String(enc, Charset.defaultCharset());
        return ret;
    }
}
