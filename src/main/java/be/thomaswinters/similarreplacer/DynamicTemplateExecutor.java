package be.thomaswinters.similarreplacer;

import com.google.common.base.Charsets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DynamicTemplateExecutor {

    /*-********************************************-*
     * EXECUTION
     *-********************************************-*/

    private static final int DEFAULT_MAX_NUMBER_OF_TRIALS = 100;

    public static void main(String[] args) throws IOException {

        String inputFileName = args[0];
        String outputFile = args[1];


        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        JsonReader reader = new JsonReader(new BufferedReader(
                new InputStreamReader(ClassLoader.getSystemResource(inputFileName).openStream(), Charsets.UTF_8)));
        List<String> corpus = gson.fromJson(reader, List.class);

        System.out.println(corpus.get(10));

        int numberOfLines = corpus.size();
        if (args.length > 2) {
            numberOfLines = Integer.parseInt(args[2]);
        }
        int maxTrials = DEFAULT_MAX_NUMBER_OF_TRIALS;
        if (args.length > 3) {
            maxTrials = Integer.parseInt(args[3]);
        }

        DynamicTemplateGenerator frequencyBasedSimilarWordReplacer = new DynamicTemplateGenerator(corpus, corpus);

        List<String> output = new ArrayList<>();
        for (int i = 0; i < numberOfLines; i++) {
            String dynamicTemplate = corpus.get(i);
            Optional<String> generated = Optional.empty();
            int trial = 0;
            while (!generated.isPresent() && trial <= maxTrials) {
                generated = frequencyBasedSimilarWordReplacer.generate(dynamicTemplate, 3, false);
                trial += 1;
                if (generated.isPresent() && corpus.contains(generated.get()) && trial <= 100) {
                    generated = Optional.empty();
                }
            }
            output.add(generated.get());
        }
        try (FileWriter writer = new FileWriter(outputFile, Charsets.UTF_8)) {
            gson.toJson(output, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
