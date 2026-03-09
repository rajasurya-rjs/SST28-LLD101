package com.example.reports;

public class RealReport implements Report {

    private final String reportId;
    private final String title;
    private final String classification;
    private final String content;

    public RealReport(String reportId, String title, String classification) {
        System.out.println("[disk] loading report " + reportId + " ...");
        try { Thread.sleep(120); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        this.reportId = reportId;
        this.title = title;
        this.classification = classification;
        this.content = "Internal report body for " + title;
    }

    @Override
    public void display(User user) {
        System.out.println("REPORT -> id=" + reportId
                + " title=" + title
                + " classification=" + classification
                + " openedBy=" + user.getName());
        System.out.println("CONTENT: " + content);
    }

    public String getClassification() {
        return classification;
    }
}
