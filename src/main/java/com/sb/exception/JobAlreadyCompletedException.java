package com.sb.exception;

public class JobAlreadyCompletedException extends  RuntimeException{

    public JobAlreadyCompletedException(String jobId) {
        super("Job cannot be modified because it is already completed: " + jobId);
    }
}
