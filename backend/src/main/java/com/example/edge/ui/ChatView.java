package com.example.edge.ui;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
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
import java.util.concurrent.CompletableFuture;

@Route("")
class ChatView extends VerticalLayout {

    private final ChatService chatService;
    private final String conversationId = UUID.randomUUID().toString();
    private final Div messages = new Div();
    private final TextField input = new TextField();
    private final Button sendButton = new Button("Send");

    ChatView(ChatService chatService) {
        this.chatService = chatService;

        setSizeFull();
        setPadding(true);
        setSpacing(false);

        Paragraph header = new Paragraph("Duke42 — Spring AI Chat");
        header.getStyle().set("font-size", "1.5em");
        header.getStyle().set("font-weight", "bold");
        add(header);

        messages.setWidthFull();
        messages.setHeightFull();
        add(messages);

        input.setWidthFull();
        input.setPlaceholder("Type your message...");
        input.addKeyDownListener(Key.ENTER, e -> sendMessage());

        sendButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        sendButton.addClickListener(e -> sendMessage());

        HorizontalLayout inputLayout = new HorizontalLayout(input, sendButton);
        inputLayout.setWidthFull();
        inputLayout.setFlexGrow(1, input);
        inputLayout.setAlignItems(FlexComponent.Alignment.CENTER);

        add(inputLayout);

        addMessage("AI", "Hello! I'm Duke42. Ask me anything.");
    }

    private void sendMessage() {
        String message = input.getValue().trim();
        if (message.isEmpty()) return;

        addMessage("You", message);
        input.clear();
        setInputEnabled(false);

        UI ui = UI.getCurrent();
        Paragraph aiMessage = new Paragraph("AI: ");
        aiMessage.getStyle().set("font-size", "0.9em");
        messages.add(aiMessage);

        CompletableFuture.runAsync(() -> {
            StringBuilder response = new StringBuilder();
            chatService.chatStream(conversationId, message)
                .doOnNext(chunk -> {
                    response.append(chunk);
                    ui.access(() -> aiMessage.setText("AI: " + response));
                })
                .doOnError(error -> ui.access(() -> {
                    setInputEnabled(true);
                    aiMessage.setText("Error: " + error.getMessage());
                }))
                .doOnComplete(() -> ui.access(() -> setInputEnabled(true)))
                .blockLast();
        });
    }

    private void setInputEnabled(boolean enabled) {
        input.setEnabled(enabled);
        sendButton.setEnabled(enabled);
    }

    private void addMessage(String sender, String text) {
        Paragraph msg = new Paragraph(sender + ": " + text);
        msg.getStyle().set("font-size", "0.9em");
        messages.add(msg);
    }
}
