package org.acme.domain;

public class JobWrapper {
    private String contents;
    private JobType type;

    public JobWrapper(String contents, JobType type) {
        this.contents = contents;
        this.type = type;
    }

    public String getContents() {
        return contents;
    }

    public void setContents(String contents) {
        this.contents = contents;
    }

    public JobType getType() {
        return type;
    }

    public void setType(JobType type) {
        this.type = type;
    }
}
