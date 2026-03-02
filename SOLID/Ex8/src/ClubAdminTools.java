interface FinanceTool {
    void addIncome(double amt, String note);
    void addExpense(double amt, String note);
}

interface MinutesTool {
    void addMinutes(String text);
}

interface EventTool {
    void createEvent(String name, double budget);
    int getEventsCount();
}
