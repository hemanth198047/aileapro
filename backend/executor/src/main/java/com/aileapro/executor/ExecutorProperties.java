package com.aileapro.executor;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aileapro.executor")
public class ExecutorProperties {

    /** Docker image used for Python runs. Pinned tag, never latest in production. */
    private String pythonImage = "python:3.12-alpine";

    private int defaultTimeoutMs = 5000;
    private int maxTimeoutMs = 15000;
    private int memoryMb = 128;
    private double cpus = 0.5;
    private int pidsLimit = 64;
    private int maxSourceChars = 50_000;
    private int maxOutputChars = 100_000;

    /** When false, refuse to run and report docker_unavailable rather than falling back. */
    private boolean dockerEnabled = true;

    public String getPythonImage() { return pythonImage; }
    public void setPythonImage(String pythonImage) { this.pythonImage = pythonImage; }
    public int getDefaultTimeoutMs() { return defaultTimeoutMs; }
    public void setDefaultTimeoutMs(int defaultTimeoutMs) { this.defaultTimeoutMs = defaultTimeoutMs; }
    public int getMaxTimeoutMs() { return maxTimeoutMs; }
    public void setMaxTimeoutMs(int maxTimeoutMs) { this.maxTimeoutMs = maxTimeoutMs; }
    public int getMemoryMb() { return memoryMb; }
    public void setMemoryMb(int memoryMb) { this.memoryMb = memoryMb; }
    public double getCpus() { return cpus; }
    public void setCpus(double cpus) { this.cpus = cpus; }
    public int getPidsLimit() { return pidsLimit; }
    public void setPidsLimit(int pidsLimit) { this.pidsLimit = pidsLimit; }
    public int getMaxSourceChars() { return maxSourceChars; }
    public void setMaxSourceChars(int maxSourceChars) { this.maxSourceChars = maxSourceChars; }
    public int getMaxOutputChars() { return maxOutputChars; }
    public void setMaxOutputChars(int maxOutputChars) { this.maxOutputChars = maxOutputChars; }
    public boolean isDockerEnabled() { return dockerEnabled; }
    public void setDockerEnabled(boolean dockerEnabled) { this.dockerEnabled = dockerEnabled; }
}
