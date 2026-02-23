public class AttendanceRule implements EligibilityRule {
    @Override
    public String check(StudentProfile s) {
        if (s.attendancePct < 75) {
            return "attendance below 75";
        }
        return null;
    }
}
