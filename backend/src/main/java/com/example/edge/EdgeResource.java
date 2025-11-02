package com.example.edge;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/edge")
public class EdgeResource {

    @Inject
    EdgeLLMService llm;

    @POST
    @Path("/infer")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String infer(String prompt) {
        return llm.infer(prompt);
    }
}