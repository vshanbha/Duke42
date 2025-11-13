package com.example.edge;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("/edge")
public class EdgeResource {

    @Inject
    EdgeLLMService llm;

    @Inject
    EdgeToolLLMService toolLlm;


    @POST
    @Path("/infer")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String infer(String prompt) {
        return llm.infer(prompt);
    }

    @POST
    @Path("/chat/{chatId}")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String chat(
        @PathParam("chatId") String chatId,
        @QueryParam("message") String message
    ) {
        if (chatId == null || chatId.trim().isEmpty()) {
            return "User ID cannot be empty.";
        }
        
        return llm.chat(chatId, message);
    }

    @POST
    @Path("/toolChat/{chatId}")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String toolChat(
        @PathParam("chatId") String chatId,
        @QueryParam("message") String message
    ) {
        if (chatId == null || chatId.trim().isEmpty()) {
            return "User ID cannot be empty.";
        }
        
        return toolLlm.chatWithTools(chatId, message);
    }

}