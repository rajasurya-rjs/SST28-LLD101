interface Checker {
    int check(Submission s);
}

interface Grader {
    int grade(Submission s);
}

interface Writer {
    String write(Submission s, int plagScore, int codeScore);
}
