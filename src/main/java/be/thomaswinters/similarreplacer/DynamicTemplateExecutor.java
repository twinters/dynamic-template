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

    public static void main(String[] args) throws IOException {

        String inputFileName = args[0];
        String outputFile = args[1];


        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonReader reader = new JsonReader(new BufferedReader(
                new InputStreamReader(ClassLoader.getSystemResource(inputFileName).openStream())));
        List<String> corpus = gson.fromJson(reader, List.class);


        int numberOfLines = corpus.size();
        if (args.length > 2) {
            numberOfLines = Integer.parseInt(args[2]);
        }

        DynamicTemplateGenerator frequencyBasedSimilarWordReplacer = new DynamicTemplateGenerator(corpus, corpus);

        List<String> output = new ArrayList<>();
        for (int i = 0; i < numberOfLines; i++) {
            System.out.println(i + ": " + corpus.get(i));
            Optional<String> generated = Optional.empty();
            int trial = 0;
            while (!generated.isPresent() && trial <= 100) {
                generated = frequencyBasedSimilarWordReplacer.generate(corpus.get(i), 5, false);
                trial += 1;
                if (generated.isPresent() && corpus.contains(generated.get()) && trial <= 100) {
                    generated = Optional.empty();
                }
            }
            output.add(generated.get());

        }
        System.out.println("Output: " + String.join("\n\n", output));
        try (FileWriter writer = new FileWriter(outputFile, Charsets.UTF_8)) {
            gson.toJson(output, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
