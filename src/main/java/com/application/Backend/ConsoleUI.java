// src/main/java/org/example/ConsoleUI.java
package com.application.Backend;

import java.util.Scanner;

public class ConsoleUI {

    private final Scanner scanner;
    // private final Console console = System.console(); // Use if masking password

    public ConsoleUI() {
        this.scanner = new Scanner(System.in);
    }

    // Updated to prompt for room details
    public String getRoomNameInput(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine();
    }

    public String getPasswordInput(String prompt) {
        System.out.print(prompt);
        // Basic password reading without masking
        return scanner.nextLine();
        /*
         // Masked password reading (doesn't work in all IDE consoles)
         if (console != null) {
             char[] passwordArray = console.readPassword();
             return new String(passwordArray);
         } else {
             System.out.println("[Warning] Cannot mask password in this console. Input will be visible.");
             return scanner.nextLine();
         }
        */
    }


    public String getUserInput(String prompt) {
        System.out.print(prompt); // Show the prompt
        return scanner.nextLine(); // Read input
    }

    // Displays a message from another user
    public void displayMessage(String sender, String text) {
        System.out.println(sender + ": " + text);
    }

    // Displays a message from the system or the local user's own message
    public void displayLocalMessage(String text) {
        System.out.println(text);
    }

    // Displays an error message
    public void displayError(String errorText) {
        System.err.println("[Error] " + errorText);
    }

    // Displays general status or system info
    public void displaySystemMessage(String infoText) {
        System.out.println("["+infoText+"]"); // Corrected [] around text
    }

    //deprecated
    public void displayFingerprint(String localUser, String peerUser, String fingerprint) {
        System.out.println("---------------- VERIFY FINGERPRINT ----------------");
        System.out.println("Comparing keys between YOU (" + localUser + ") and " + peerUser + ":");
        System.out.println("Fingerprint:  " + fingerprint);
        System.out.println("VERIFY THIS with " + peerUser + " OUT-OF-BAND.");
        System.out.println("Type 'verify " + peerUser + "' to confirm match.");
        System.out.println("----------------------------------------------------");
    }

    public void close() {
        System.out.println("[UI] Closing console input.");
        scanner.close();
    }
}
