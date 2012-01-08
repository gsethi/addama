package org.systemsbiology.addama.services.execution.util;

import org.systemsbiology.addama.commons.web.exceptions.ResourceNotFoundException;
import org.systemsbiology.addama.services.execution.jobs.Job;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.logging.Logger;

/**
 * @author hrovira
 */
public class IOJob {
    private static final Logger log = Logger.getLogger(IOJob.class.getName());
    // TODO : Configure buffer size
    public static final Integer BUFFER_SIZE = 8096;

    public static void mkdirs(File... dirs) {
        if (dirs != null) {
            for (File dir : dirs) {
                if (!dir.exists()) {
                    log.fine(dir.getPath() + ":" + dir.mkdirs());
                } else {
                    log.fine(dir.getPath() + ": already exists");
                }
            }
        }
    }

    public static void recursiveDelete(File f) {
        if (f.exists()) {
            if (f.isDirectory()) {
                for (File subf : f.listFiles()) {
                    recursiveDelete(subf);
                }
            }

            log.fine("deleting:" + f.getPath());
            if (!f.delete()) {
                log.warning("deleting:" + f.getPath() + ": may not have happened");
            }
        }
    }

    public static String getLogContents(File logFile) {
        StringBuilder builder = new StringBuilder();
        BufferedReader logReader = null;
        try {
            logReader = new BufferedReader(new InputStreamReader(new FileInputStream(logFile)));
            String line = "";
            while (line != null) {
                line = logReader.readLine();
                if (line != null) {
                    builder.append(line).append("\n");
                }
            }
        } catch (Exception e) {
            log.warning(logFile + ":" + e);
        } finally {
            try {
                if (logReader != null) {
                    logReader.close();
                }
            } catch (IOException e) {
                log.warning(logFile + ":" + e);
            }
        }
        return builder.toString();
    }

    public static void outputFile(Job job, String filepath, HttpServletResponse response, ServletContext servletContext)
            throws ResourceNotFoundException, IOException {
        File outputDir = new File(job.getOutputDirectoryPath());
        if (!outputDir.exists()) {
            throw new ResourceNotFoundException(job.getJobUri() + "/outputs");
        }

        File outputFile = new File(job.getOutputDirectoryPath(), filepath);
        if (!outputFile.exists()) {
            throw new ResourceNotFoundException(job.getJobUri() + "/outputs/_afdl/" + filepath);
        }

        response.setContentType(servletContext.getMimeType(outputFile.getName()));

        InputStream inputStream = new FileInputStream(outputFile);
        OutputStream outputStream = response.getOutputStream();

        byte[] buffer = new byte[BUFFER_SIZE];
        while (true) {
            int bytesRead = inputStream.read(buffer);
            if (bytesRead == -1) {
                break;
            }
            outputStream.write(buffer, 0, bytesRead);
        }
    }

}
