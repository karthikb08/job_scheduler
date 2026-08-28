package com.sb.service;

import com.sb.model.Job;

import  com.sb.model.JobType;

public interface JobHandler {

    JobType supportedType();
    void handle(Job job);
}