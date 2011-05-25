/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package homenetapp;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.text.*;

import java.util.*;
import java.util.regex.*;
import java.util.zip.*;
import java.lang.Throwable;
import homenet.*;

//import org.apache.xmlrpc.XmlRpcClient;


import org.apache.commons.configuration.*;

/**
 *
 * @author mdoll
 */
public class HomeNetApp {

    public HashMap<Integer, String[]> commands;
    public PropertiesConfiguration config;
    
    private homenet.Stack _homenet;
    private SerialManager _serialmanager;
    
    private XmlrpcClient _xmlrpcClient;
    private XmlrpcServer _xmlrpcServer;
    
        
    // public HashMap Por;

    public HomeNetApp() {
        
        loadCommands();
        loadConfig();
        
        
        
        _homenet = new homenet.Stack(0xff);
        
        _homenet.init();
        
        HashMap<String,Port> ports = new HashMap<String,Port>();
        HashMap<Integer,Device> devices = new HashMap<Integer,Device>();
        
       boolean loadXmlrpc = true;
        
        try {
            _xmlrpcClient = new XmlrpcClient("homenet.me");
            _xmlrpcServer = new XmlrpcServer(2443);
        } catch(Exception e){
            System.out.println(e.getMessage());
            loadXmlrpc = false;
        }    
        
        _serialmanager = new SerialManager(_homenet);
    }
    
    private boolean loadConfig() {
        try {
            config = new PropertiesConfiguration("homenet.properties.txt");
        } catch(Exception e){
            return false;
        }
        return true;
    }

    public void saveConfig() {
        try{
            config.save();
        } catch(Exception e){
            System.out.println("Can't save config");
        }
    }
    
    private void loadCommands() {
        commands = new HashMap<Integer, String[]>();

        String[] strings = loadStrings("commands.txt");

        for (int i = 0; i < strings.length; i++) {
            String[] r = splitTokens(strings[i], "\t");
            commands.put(Integer.parseInt(r[0], 16), r);
        }
        System.out.println("Size: "+commands.size());
        System.out.println("Length: "+strings.length);
    }
        
    class compareCommands implements Comparator {

        public int compare(Object i1, Object i2) {
            return ((Integer) i1).intValue() - ((Integer) i2).intValue();
        }
    }

    public Object[] getCommandKeys() {

        //System.out.println(commands.keySet().size());
       // System.out.println("Size: "+commands.size());
        
       // Object[] rows = commands.keySet().toArray();
      //  Arrays.sort(rows, new compareCommands());
      //  System.out.println("Length: "+rows.length);
        
        
       //Object[] rows = new Object[1]; 
       Integer[] test =  {new Integer(2),new Integer(2),new Integer(2),new Integer(2)};
       
       // System.exit(-1);
        return test;

    }

    public String getAppPath(String filename) {
        //@todo find the right path
        String path = HomeNetApp.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        System.out.println(path + filename);
        return path + filename;
        //return "C:\\Users\\mdoll\\Documents\\NetBeansProjects\\HomeNet.me-App\\" + filename;
       // return "C:\\Projects (Safe)\\HomeNet.me-App\\" + filename;
    }
    
    public void exit(){
        saveConfig();
    }

    

    

    class SerialManager {

        public ArrayList portList = new ArrayList();
        private SerialCheckThread sThread;
        private homenet.Stack _homeNet;

        public SerialManager(homenet.Stack stack) {
            
            _homeNet = stack;
            
            //loadSerialPorts();
            checkSerialPorts();
            

            sThread = new SerialCheckThread(1000);
            sThread.setPriority(3);
            sThread.start();

        }

        public void checkSerialPorts() {

            ArrayList currentList = Serial.listPorts();
            HashMap currentPorts = _homeNet.getPorts();

            //check for new ports
            Iterator i = currentList.iterator();
            while (i.hasNext()) {
                String k = (String) i.next();

                if (portList.contains(k)) {
                    portList.add(k);
                }
            }
           

            //check for removed ports
            
            //loops throught the old list and compares it to the new one
            
            i = portList.iterator();
            while (i.hasNext()) {
                String k = (String) i.next();

                if (!currentList.contains(k)) {
                    
                    //remove from portlist too;
                    System.out.println("auto remove " + k);
                    if (currentPorts.containsKey(k)) {
                        _homeNet.removePort(k);
                    }
                    portList.remove(k);
                    //redrawGUI = true;
                    System.out.println("Port " + k + " was Disconnected");
                }
            }
        }

        public void loadSerialPorts() {
          
        }    

        void saveSerialPorts() {
           config.setProperty("test","test");
        }

//based on SimpleThread
        class SerialCheckThread extends Thread {

            boolean running;           // Is the thread running?  Yes or no?
            int wait;                  // How many milliseconds should we wait in between executions?
            String id;                 // Thread name
            int count;                 // counter
            boolean check;

            // Constructor, create the thread
            // It is not running by default
            SerialCheckThread(int w) {
                wait = w;
                running = false;
                check = true;
            }

            // Overriding "start()"
            public void start() {
                // Set running equal to true
                running = true;

                check = true;
                // Print messages
                System.out.println("Starting Serial Port Check Thread (will execute every " + wait + " milliseconds.)");
                // Do whatever start does in Thread, don't forget this!
                super.start();
            }

            void startChecking() {
                check = true;
            }

            void stopChecking() {
                check = false;
            }

            // We must implement run, this gets triggered by start()
            public void run() {
                while (running) {
                    if (check == true) {
                        checkSerialPorts();
                    }
                    // Ok, let's wait for however long we should wait
                    try {
                        Thread.sleep((long) (wait));
                    } catch (Exception e) {
                    }
                }
                System.out.println(id + " thread is done!");  // The thread is done when we get to the end of run()
            }

            // Our method that quits the thread
            public void quit() {
                System.out.println("Quitting.");
                running = false;  // Setting running to false ends the loop in run()
                // IUn case the thread is waiting. . .
                interrupt();
            }
        }
    }
    
    
    
    
    
    
    
    
    
//helper functions based on functions from processing.org
    static final String WHITESPACE = " \t\n\r\f\u00A0";

    /**
     * Call openStream() without automatic gzip decompression.
     */
    public InputStream createInputRaw(String filename) {
        InputStream stream = null;

        if (filename == null) {
            return null;
        }

        if (filename.length() == 0) {
            // an error will be called by the parent function
            //System.err.println("The filename passed to openStream() was empty.");
            return null;
        }

        // safe to check for this as a url first. this will prevent online
        // access logs from being spammed with GET /sketchfolder/http://blahblah
        if (filename.indexOf(":") != -1) {  // at least smells like URL
            try {
                URL url = new URL(filename);
                stream = url.openStream();
                return stream;

            } catch (MalformedURLException mfue) {
                // not a url, that's fine
            } catch (FileNotFoundException fnfe) {
                // Java 1.5 likes to throw this when URL not available. (fix for 0119)
                // http://dev.processing.org/bugs/show_bug.cgi?id=403
            } catch (IOException e) {
                // changed for 0117, shouldn't be throwing exception
                e.printStackTrace();
                //System.err.println("Error downloading from URL " + filename);
                return null;
                //throw new RuntimeException("Error downloading from URL " + filename);
            }
        }

        // Moved this earlier than the getResourceAsStream() checks, because
        // calling getResourceAsStream() on a directory lists its contents.
        // http://dev.processing.org/bugs/show_bug.cgi?id=716
        try {
            // First see if it's in a data folder. This may fail by throwing
            // a SecurityException. If so, this whole block will be skipped.
            File file = new File(getAppPath(filename));

            if (file.isDirectory()) {
                return null;
            }
            if (file.exists()) {
                try {
                    // handle case sensitivity check
                    String filePath = file.getCanonicalPath();
                    String filenameActual = new File(filePath).getName();
                    // make sure there isn't a subfolder prepended to the name
                    String filenameShort = new File(filename).getName();
                    // if the actual filename is the same, but capitalized
                    // differently, warn the user.
                    //if (filenameActual.equalsIgnoreCase(filenameShort) &&
                    //!filenameActual.equals(filenameShort)) {
                    if (!filenameActual.equals(filenameShort)) {
                        throw new RuntimeException("This file is named "
                                + filenameActual + " not "
                                + filename + ". Rename the file "
                                + "or change your code.");
                    }
                } catch (IOException e) {
                }
            }

            // if this file is ok, may as well just load it
            stream = new FileInputStream(file);
            if (stream != null) {
                return stream;
            }

            // have to break these out because a general Exception might
            // catch the RuntimeException being thrown above
        } catch (IOException ioe) {
        } catch (SecurityException se) {
        }

        // Using getClassLoader() prevents java from converting dots
        // to slashes or requiring a slash at the beginning.
        // (a slash as a prefix means that it'll load from the root of
        // the jar, rather than trying to dig into the package location)
        ClassLoader cl = getClass().getClassLoader();

        // by default, data files are exported to the root path of the jar.
        // (not the data folder) so check there first.
        stream = cl.getResourceAsStream("data/" + filename);
        if (stream != null) {
            String cn = stream.getClass().getName();
            // this is an irritation of sun's java plug-in, which will return
            // a non-null stream for an object that doesn't exist. like all good
            // things, this is probably introduced in java 1.5. awesome!
            // http://dev.processing.org/bugs/show_bug.cgi?id=359
            if (!cn.equals("sun.plugin.cache.EmptyInputStream")) {
                return stream;
            }
        }

        // When used with an online script, also need to check without the
        // data folder, in case it's not in a subfolder called 'data'.
        // http://dev.processing.org/bugs/show_bug.cgi?id=389
        stream = cl.getResourceAsStream(filename);
        if (stream != null) {
            String cn = stream.getClass().getName();
            if (!cn.equals("sun.plugin.cache.EmptyInputStream")) {
                return stream;
            }
        }

        try {
            // attempt to load from a local file, used when running as
            // an application, or as a signed applet
            try {  // first try to catch any security exceptions
                try {
                    stream = new FileInputStream(getAppPath(filename));
                    if (stream != null) {
                        return stream;
                    }
                } catch (IOException e2) {
                }



                try {
                    stream = new FileInputStream(filename);
                    if (stream != null) {
                        return stream;
                    }
                } catch (IOException e1) {
                }

            } catch (SecurityException se) {
            }  // online, whups

        } catch (Exception e) {
            //die(e.getMessage(), e);
            e.printStackTrace();
        }

        return null;
    }

    public InputStream createInput(String filename) {
        InputStream input = createInputRaw(filename);
        if ((input != null) && filename.toLowerCase().endsWith(".gz")) {
            try {
                return new GZIPInputStream(input);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
        return input;
    }

    static public InputStream createInput(File file) {
        if (file == null) {
            throw new IllegalArgumentException("File passed to createInput() was null");
        }
        try {
            InputStream input = new FileInputStream(file);
            if (file.getName().toLowerCase().endsWith(".gz")) {
                return new GZIPInputStream(input);
            }
            return input;

        } catch (IOException e) {
            System.err.println("Could not createInput() for " + file);
            e.printStackTrace();
            return null;
        }
    }

    public String[] loadStrings(String filename) {
        InputStream is = createInput(filename);
        if (is != null) {
            return loadStrings(is);
        }

        System.err.println("The file \"" + filename + "\" "
                + "is missing or inaccessible, make sure "
                + "the URL is valid or that the file has been "
                + "added to your sketch and is readable.");
        return null;
    }

    static public String[] loadStrings(InputStream input) {
        try {
            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(input, "UTF-8"));

            String lines[] = new String[100];
            int lineCount = 0;
            String line = null;
            while ((line = reader.readLine()) != null) {
                if (lineCount == lines.length) {
                    String temp[] = new String[lineCount << 1];
                    System.arraycopy(lines, 0, temp, 0, lineCount);
                    lines = temp;
                }
                lines[lineCount++] = line;
            }
            reader.close();

            if (lineCount == lines.length) {
                return lines;
            }

            // resize array to appropriate amount for these lines
            String output[] = new String[lineCount];
            System.arraycopy(lines, 0, output, 0, lineCount);
            return output;

        } catch (IOException e) {
            e.printStackTrace();
            //throw new RuntimeException("Error inside loadStrings()");
        }
        return null;
    }

    /**
     * Split the provided String at wherever whitespace occurs.
     * Multiple whitespace (extra spaces or tabs or whatever)
     * between items will count as a single break.
     * <P>
     * The whitespace characters are "\t\n\r\f", which are the defaults
     * for java.util.StringTokenizer, plus the unicode non-breaking space
     * character, which is found commonly on files created by or used
     * in conjunction with Mac OS X (character 160, or 0x00A0 in hex).
     * <PRE>
     * i.e. splitTokens("a b") -> { "a", "b" }
     *      splitTokens("a    b") -> { "a", "b" }
     *      splitTokens("a\tb") -> { "a", "b" }
     *      splitTokens("a \t  b  ") -> { "a", "b" }</PRE>
     */
    static public String[] splitTokens(String what) {
        return splitTokens(what, WHITESPACE);
    }

    /**
     * Splits a string into pieces, using any of the chars in the
     * String 'delim' as separator characters. For instance,
     * in addition to white space, you might want to treat commas
     * as a separator. The delimeter characters won't appear in
     * the returned String array.
     * <PRE>
     * i.e. splitTokens("a, b", " ,") -> { "a", "b" }
     * </PRE>
     * To include all the whitespace possibilities, use the variable
     * WHITESPACE, found in PConstants:
     * <PRE>
     * i.e. splitTokens("a   | b", WHITESPACE + "|");  ->  { "a", "b" }</PRE>
     */
    static public String[] splitTokens(String what, String delim) {
        StringTokenizer toker = new StringTokenizer(what, delim);
        String pieces[] = new String[toker.countTokens()];

        int index = 0;
        while (toker.hasMoreTokens()) {
            pieces[index++] = toker.nextToken();
        }
        return pieces;
    }
}