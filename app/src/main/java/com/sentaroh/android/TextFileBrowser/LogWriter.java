package com.sentaroh.android.TextFileBrowser;
/*
The MIT License (MIT)
Copyright (c) 2011 Sentaroh

Permission is hereby granted, free of charge, to any person obtaining a copy of
this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights to use,
copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
and to permit persons to whom the Software is furnished to do so, subject to
the following conditions:

The above copyright notice and this permission notice shall be included in all copies or
substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
OTHER DEALINGS IN THE SOFTWARE.

*/

import android.content.Context;
import android.util.Log;

import com.sentaroh.android.Utilities3.StringUtil;
import com.sentaroh.android.Utilities3.ThreadCtrl;

import org.slf4j.LoggerWriter;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.ArrayBlockingQueue;

import static com.sentaroh.android.TextFileBrowser.Constants.APPLICATION_TAG;

public class LogWriter extends LoggerWriter {
    final private static int LOG_BUFFER_SIZE=1024*512;
    final private static int LOG_FILE_MAX_SIZE=1024*1024*10;
    final private static int LOG_FILE_MAX_COUNT=20;

    final public static String LOG_FILE_NAME_ARCHIVE_PREFIX="log_";

    static private PrintWriter logPrinter=null;
    static private long logFileFlushSize =0L;
    static private File logFile=null;
    static private String logDirectory="";

    private ArrayBlockingQueue<String> mLogMessageQueue =new ArrayBlockingQueue<String>(5000);

    public LogWriter() { }

    @Override
    public void write(String msg) {
        writeMsg(msg);
    }

    public void logInit(Context c) {
        logDirectory=c.getExternalFilesDirs(null)[0].getPath()+"/log/";
        File log_dir=new File(logDirectory);
        if (!log_dir.exists()) log_dir.mkdirs();
        logOpen();
    }

    public String logGetLogDirectory() {
        return logDirectory;
    }

    public void logFlush() {
        if (logPrinter!=null) logPrinter.flush();
    }

    public void logClose() {
        if (logPrinter!=null) {
//            Log.v(APPLICATION_TAG, "Log closed");
            logPrinter.flush();
            logPrinter.close();
            logPrinter=null;
            logFile=null;
            logFileFlushSize =0L;
        }
    }

    public void logOpen() {
        if (logPrinter==null) {
//            Log.v(APPLICATION_TAG, "Log opened");
            SimpleDateFormat sdf=new SimpleDateFormat("yyyyMMdd_HHmmss");
            logFile=new File(logDirectory+LOG_FILE_NAME_ARCHIVE_PREFIX+sdf.format(System.currentTimeMillis())+".txt");
            logCreatePrinter();
            houseKeepLogFile();
        }
    }

    private void logCreatePrinter() {
        try {
            logFileFlushSize =0L;
            FileOutputStream fos=new FileOutputStream(logFile);
            BufferedOutputStream bos=new BufferedOutputStream(fos, LOG_BUFFER_SIZE);
            logPrinter=new PrintWriter(bos);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            logPrinter=null;
        }
    }

    public void logRotate() {
        if (logPrinter!=null) {
//            Log.v(APPLICATION_TAG,"log rotated");
            logClose();
            logOpen();
            houseKeepLogFile();
            logCreatePrinter();
        }
    }

    public boolean isLogFileExists() {
        File log_dir=new File(logDirectory);
        if (log_dir.exists()) {
            File[] fl=log_dir.listFiles();
            ArrayList<File> log_fl=new ArrayList<File>();
            for(File item:fl) {
                if (item.getName().startsWith(LOG_FILE_NAME_ARCHIVE_PREFIX)) {
                    return true;
                }
            }
        }
        return false;
    }


    public boolean isLogRemovableFileExists() {
        File log_dir=new File(logDirectory);
        if (log_dir.exists()) {
            File[] fl=log_dir.listFiles();
            ArrayList<File> log_fl=new ArrayList<File>();
            for(File item:fl) {
                if (item.getName().startsWith(LOG_FILE_NAME_ARCHIVE_PREFIX)) {
                    if (logFile!=null) {
                        if (!item.getName().equals(logFile.getName())) {
                            return true;
                        }
                    } else {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public void logRemoveFile() {
        File log_dir=new File(logDirectory);
        if (log_dir.exists()) {
            File[] fl=log_dir.listFiles();
            ArrayList<File> log_fl=new ArrayList<File>();
            for(File item:fl) {
                if (item.getName().startsWith(LOG_FILE_NAME_ARCHIVE_PREFIX)) {
                    if (logFile!=null) {
//                        Log.v(APPLICATION_TAG, "item="+item.getName()+", logFile="+logFile.getName());
                        if (!item.getName().equals(logFile.getName())) {
                            item.delete();
//                            Log.v(APPLICATION_TAG, "log file was deleted, fp="+item.getPath());
                        }
                    } else {
                        item.delete();
//                        Log.v(APPLICATION_TAG, "log file was deleted, fp="+item.getPath());
                    }
                }
            }
        }
    }

    private void houseKeepLogFile() {
        File log_dir=new File(logDirectory);
        if (log_dir.exists()) {
            File[] fl=log_dir.listFiles();
            ArrayList<File> log_fl=new ArrayList<File>();
            for(File item:fl) {
                if (item.getName().startsWith(LOG_FILE_NAME_ARCHIVE_PREFIX)) {
                    log_fl.add(item);
                }
            }
            Collections.sort(log_fl, new Comparator<File>(){
                @Override
                public int compare(File o1, File o2) {
                    return o1.getName().compareToIgnoreCase(o2.getName());
                }
            });
            if (log_fl.size()>=LOG_FILE_MAX_COUNT) {
                while(log_fl.size()>=LOG_FILE_MAX_COUNT) {
                    log_fl.get(0).delete();
                    log_fl.remove(0);
                }
            }
        }
    }

    private static Thread logWriteThread=null;
    private static ThreadCtrl logWriterTc=new ThreadCtrl();
    private void writeMsg(String msg) {
        if (logPrinter!=null) {
            Log.v(APPLICATION_TAG,msg);
            String dt_msg= StringUtil.convDateTimeTo_YearMonthDayHourMinSecMili(System.currentTimeMillis())+" "+msg;
            mLogMessageQueue.add(dt_msg);
            if (logWriteThread==null) {
                createLogThread();
            } else {
                synchronized (logWriterTc) {
//                    Log.v(APPLICATION_TAG, "logWriterTherad notified");
                    logWriterTc.notify();
                }
            }
        }
    }

    private void createLogThread() {
        logWriteThread=new Thread() {
            @Override
            public void run() {
                Log.v(APPLICATION_TAG, "logWriterTherad started");
                while(mLogMessageQueue.size()>0) {
                    if (logPrinter!=null) {
                        synchronized (logPrinter) {
                            String l_msg=mLogMessageQueue.poll();
                            if (l_msg!=null) {
                                logPrinter.println(l_msg);
                                logFileFlushSize +=l_msg.length();
                                if (logFileFlushSize>=LOG_BUFFER_SIZE) {
                                    logPrinter.flush();
                                    logFileFlushSize =0L;
//                                Log.v(APPLICATION_TAG,"Log file flushed");
                                    if (logFile.length()>=LOG_FILE_MAX_SIZE) {
//                                    Log.v(APPLICATION_TAG,"Log file rotated");
                                        logRotate();
                                    }
                                }
                            }
                        }
                        if (mLogMessageQueue.size()==0) {
                            logFlush();
                            synchronized (logWriterTc) {
                                try {
                                    logWriterTc.wait(1000*30);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }
                logWriteThread=null;
                Log.v(APPLICATION_TAG, "logWriterTherad ended");
            }
        };
        logWriteThread.start();
    }
}