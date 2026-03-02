package com.example.tickets;

import java.util.ArrayList;
import java.util.List;

public class TicketService {

    public IncidentTicket createTicket(String id, String reporterEmail, String title) {
        List<String> defaultTags = new ArrayList<>();
        defaultTags.add("NEW");

        return new IncidentTicket.Builder()
                .id(id)
                .reporterEmail(reporterEmail)
                .title(title)
                .priority("MEDIUM")
                .source("CLI")
                .customerVisible(false)
                .tags(defaultTags)
                .build();
    }

    public IncidentTicket escalateToCritical(IncidentTicket t) {
        List<String> newTags = new ArrayList<>(t.getTags());
        newTags.add("ESCALATED");

        return t.toBuilder()
                .priority("CRITICAL")
                .tags(newTags)
                .build();
    }

    public IncidentTicket assign(IncidentTicket t, String assigneeEmail) {
        return t.toBuilder()
                .assigneeEmail(assigneeEmail)
                .build();
    }
}
