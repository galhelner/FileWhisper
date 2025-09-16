package com.example.ai_poweredtextanalyzer.Objects;

/**
 * This class represent a single message of the chat between the user and the AI Model.
 */
public class ChatMessage {
    private final String sender;
    private final String text;


    public ChatMessage(String sender, String text) {
        if (sender.equals("user")) {
            this.sender = "Me";
        } else if (sender.equals("assistant")) {
            this.sender = "AI Model";
        } else {
            this.sender = sender;
        }

        this.text = text;
    }


    public String getSender() {
        return sender;
    }

    public String getText() {
        return text;
    }
}
