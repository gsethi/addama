package org.systemsbiology.addama.services.execution.scheduling;

import org.systemsbiology.addama.services.execution.io.Streamer;
import org.systemsbiology.addama.services.execution.jobs.Job;
import org.systemsbiology.addama.services.execution.jobs.JobPackage;
import org.systemsbiology.addama.services.execution.jobs.JobStatus;

import java.io.*;
import java.util.Queue;
import java.util.logging.Logger;

import static java.lang.Runtime.getRuntime;
import static java.lang.Thread.sleep;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.systemsbiology.addama.services.execution.jobs.JobStatus.*;
import static org.systemsbiology.addama.services.execution.util.HttpJob.getScriptExecution;

/**
 * @author hrovira
 */
public class JobQueueHandlingRunnable implements Runnable {
    private static final Logger log = Logger.getLogger(JobQueueHandlingRunnable.class.getName());

    private final Queue<JobPackage> jobQueue;
    private final ProcessRegistry processRegistry;
    private final String[] environmentVariables;

    public JobQueueHandlingRunnable(Queue<JobPackage> jobQueue, ProcessRegistry processRegistry, String[] vars) {
        this.jobQueue = jobQueue;
        this.processRegistry = processRegistry;
        this.environmentVariables = (vars != null) ? vars : new String[0];
    }

    public void run() {
        while (true) {
            pause();

            JobPackage jobPackage = jobQueue.poll();
            if (jobPackage != null) {
                Closeable[] streams = null;
                try {
                    Job job = jobPackage.retrieve();

                    JobStatus currentStatus = job.getJobStatus();
                    if (currentStatus.equals(stopping)) {
                        throw new InterruptedException("stopping: " + jobPackage.getJobUri());
                    }

                    if (!currentStatus.equals(scheduled)) {
                        continue;
                    }

                    Process p = createProcess(job);
                    processRegistry.start(jobPackage, p);

                    streams = initStreams(p, job);

                    jobPackage.changeStatus(running);

                    int exitValue = p.waitFor();

                    jobPackage.completed(exitValue);

                } catch (AlreadyScheduledJobException e) {
                    log.warning(e.getMessage());

                } catch (InterruptedException e) {
                    log.info(e.getMessage());
                    jobPackage.changeStatus(stopped);

                } catch (Exception e) {
                    log.warning(e.getMessage());
                    e.printStackTrace();

                    jobPackage.onError(e);
                } finally {
                    closeStreams(streams);
                    processRegistry.end(jobPackage);
                }
            }
        }
    }

    /*
    * Private Methods
    */

    private Process createProcess(Job job) throws Exception {
        log.info(job.toString());

        if (isEmpty(job.getScriptPath())) {
            throw new Exception("unable to run job without script path:" + job.getJobUri());
        }

        return getRuntime().exec(getScriptExecution(job), environmentVariables, new File(job.getExecutionDirectory()));
    }

    private Closeable[] initStreams(Process p, Job job) throws FileNotFoundException {
        OutputStream logStream = new FileOutputStream(job.getLogPath());
        Streamer stdoutStreamer = new Streamer(p.getInputStream(), logStream, true);
        Streamer errorStreamer = new Streamer(p.getErrorStream(), logStream, true);

        new Thread(stdoutStreamer).start();
        new Thread(errorStreamer).start();

        return new Closeable[]{stdoutStreamer, errorStreamer, logStream};
    }

    private void closeStreams(Closeable... closeables) {
        if (closeables != null) {
            for (Closeable closeable : closeables) {
                try {
                    closeable.close();
                } catch (Exception e) {
                    log.warning(e.getMessage());
                }
            }
        }
    }

    private void pause() {
        try {
            sleep(1000);
        } catch (InterruptedException e) {
            log.warning(e.getMessage());
        }
    }
}
