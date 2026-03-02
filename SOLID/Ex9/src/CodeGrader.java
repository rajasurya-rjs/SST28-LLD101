public class CodeGrader implements Grader {
    public int grade(Submission s) {
        Rubric r = new Rubric();
        int base = Math.min(80, 50 + s.code.length() % 40);
        return base + r.bonus;
    }
}
