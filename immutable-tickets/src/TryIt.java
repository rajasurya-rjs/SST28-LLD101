import com.example.tickets.IncidentTicket;
import com.example.tickets.TicketService;

import java.util.List;

public class TryIt {

    public static void main(String[] args) {
        TicketService service = new TicketService();

        IncidentTicket t = service.createTicket("TCK-1001", "reporter@example.com", "Payment failing on checkout");
        System.out.println("Created: " + t);

        IncidentTicket t2 = service.assign(t, "agent@example.com");
        System.out.println("\nAfter assign (new ticket): " + t2);
        System.out.println("Original unchanged:        " + t);

        IncidentTicket t3 = service.escalateToCritical(t2);
        System.out.println("\nAfter escalate (new ticket): " + t3);
        System.out.println("Previous unchanged:          " + t2);

        List<String> tags = t3.getTags();
        try {
            tags.add("HACKED_FROM_OUTSIDE");
            System.out.println("\nOops, that shouldn't have worked");
        } catch (UnsupportedOperationException e) {
            System.out.println("\nCannot modify tags from outside - immutability works!");
        }

        System.out.println("Tags still safe: " + t3.getTags());
    }
}
