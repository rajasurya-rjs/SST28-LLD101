package com.example.pen;

public class App {

    public static void main(String[] args) {

        // === 1. Factory: Create different pen types ===
        System.out.println("=== Ink Pen with Cap ===");
        Pen inkPen = PenFactory.getPen("ink-pen", "blue", "with-cap");
        inkPen.start();
        inkPen.write("Hello from ink pen");
        inkPen.close();

        System.out.println();
        System.out.println("=== Ball Pen with Click ===");
        Pen ballPen = PenFactory.getPen("ball-pen", "black", "click-mechanism");
        ballPen.start();
        ballPen.write("Hello from ball pen");
        ballPen.close();

        System.out.println();
        System.out.println("=== Gel Pen with Cap ===");
        Pen gelPen = PenFactory.getPen("gel-pen", "red", "with-cap");
        gelPen.start();
        gelPen.write("Hello from gel pen");
        gelPen.close();

        // === 2. Refill: Change color ===
        System.out.println();
        System.out.println("=== Refill Demo ===");
        gelPen.refill("green");
        gelPen.start();
        gelPen.write("Now writing in green");
        gelPen.close();

        // === 3. Decorator: Grip functionality ===
        System.out.println();
        System.out.println("=== Grip Decorator Demo ===");
        Pen pen = PenFactory.getPen("gel-pen", "purple", "click-mechanism");
        Writable grippedPen = new GripDecorator(pen);
        grippedPen.start();
        grippedPen.write("Writing with grip support");
        grippedPen.close();

        // === 4. ISP: Pencil (no refill) ===
        System.out.println();
        System.out.println("=== Pencil Demo (ISP — no refill) ===");
        Pencil pencil = new Pencil();
        pencil.start();
        pencil.write("Hello from pencil");
        pencil.close();

        // === 5. Polymorphism: Both Pen and Pencil as Writable ===
        System.out.println();
        System.out.println("=== Polymorphism Demo ===");
        Writable[] tools = {
            PenFactory.getPen("ink-pen", "blue", "with-cap"),
            new Pencil(),
            new GripDecorator(PenFactory.getPen("ball-pen", "black", "click-mechanism"))
        };
        for (Writable tool : tools) {
            tool.start();
            tool.write("Same interface, different behavior");
            tool.close();
            System.out.println();
        }
    }
}
