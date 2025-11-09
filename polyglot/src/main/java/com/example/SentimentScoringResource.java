package com.example;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.InputStreamReader;
import java.io.Reader;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.graalvm.python.embedding.GraalPyResources;

@Path("/polyglot")
public class SentimentScoringResource {

    private static final String PYTHON = "python";

    private static Value scoreFunction;

    private final String pythonScriptPath = "load_model.py";

    Context polyglotContext;

    @PostConstruct
    void setup() {
        Source source;

        try {
            polyglotContext = GraalPyResources.createContext();
            // Load the Python script from classpath
            try (Reader reader = new InputStreamReader(AnomalyDetectionResource.class.getClassLoader().getResourceAsStream(pythonScriptPath))) {
                source = Source.newBuilder(PYTHON, reader, pythonScriptPath).build();
            } catch (Exception e) {
                System.err.println("Error loading Python source script: " + e.getMessage());
                e.printStackTrace();
                return;
            }
            System.err.println("Python source script file found successfully.");

            polyglotContext.eval(source);
            System.err.println("Python script loaded successfully.");
//            polyglotContext.eval(PYTHON, "from load_model import load_model_and_score; score_function = load_model_and_score()");
            scoreFunction = polyglotContext.getBindings("python").getMember("analyze_sentiment");
            if (scoreFunction == null || scoreFunction.isNull()) {
                System.err.println("Failed to load score function from Python script.");
                return;
            }
            System.out.println("scoreFunction script loaded successfully.");

        } catch (Exception e) {
            System.err.println("Error initializing Python context: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @POST
    @Path("/score")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response score(String text) {
        System.err.println("Scoring Function value : " + scoreFunction);
        if (scoreFunction == null || !scoreFunction.canExecute()) {
            System.err.println("Score function is not properly initialized.");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Python function not initialized.")
                .type(MediaType.TEXT_PLAIN)
                .build();
        }

        try {
            // Convert the text data to a Polyglot value
            Value textValue = polyglotContext.asValue(text);
            // Call the Python scoring function
            Value result = scoreFunction.execute(textValue);

            // Convert the result to a Java String
            return Response.ok(result.asString(), MediaType.TEXT_PLAIN).build();
        } catch (Exception e) {
            System.err.println("Error during scoring: " + e.getMessage());
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Error scoring text:" + e.getMessage())
                .type(MediaType.TEXT_PLAIN)
                .build();
        }
    }

	@PreDestroy
	public void close() {
		if (polyglotContext != null) {
			polyglotContext.close(true);
		}
	}    

}
