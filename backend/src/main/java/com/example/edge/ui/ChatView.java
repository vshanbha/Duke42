package com.example.edge.ui;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;

import java.util.UUID;

@Route("")
class ChatView extends VerticalLayout {

    private final ChatService chatService;
    private final String conversationId = UUID.randomUUID().toString();
    private final Div messages = new Div();
    private final TextField input = new TextField();

    ChatView(ChatService chatService) {
        this.chatService = chatService;

        setSizeFull();
        setPadding(true);
        setSpacing(false);

        // Header
        Paragraph header = new Paragraph("Duke42 — Spring AI Chat");
        header.getStyle().set("font-size", "1.5em");
        header.getStyle().set("font-weight", "bold");
        add(header);

        // Messages area
        messages.setWidthFull();
        messages.setHeightFull();
        add(messages);

        // Input area
        input.setWidthFull();
        input.setPlaceholder("Type your message...");
        input.addKeyDownListener(Key.ENTER, e -> sendMessage());

        Button sendButton = new Button("Send");
        sendButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        sendButton.addClickListener(e -> sendMessage());

        HorizontalLayout inputLayout = new HorizontalLayout(input, sendButton);
        inputLayout.setWidthFull();
        inputLayout.setFlexGrow(1, input);
        inputLayout.setAlignItems(FlexComponent.Alignment.CENTER);

        add(inputLayout);

        // Welcome message
        addMessage("AI", "Hello! I'm Duke42. Ask me anything.");
    }

    private void sendMessage() {
        String message = input.getValue().trim();
        if (message.isEmpty()) return;

        addMessage("You", message);
        input.clear();

        // Run LLM call in background
        try {
            String response = chatService.chat(conversationId, message);
            addMessage("AI", response);
        } catch (Exception e) {
            addMessage("Error", e.getMessage());
        }
    }

    private void addMessage(String sender, String text) {
        Paragraph msg = new Paragraph(sender + ": " + text);
        msg.getStyle().set("font-size", "0.9em");
        messages.add(msg);
    }
}
