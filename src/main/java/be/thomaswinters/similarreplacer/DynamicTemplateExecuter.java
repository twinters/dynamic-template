package be.thomaswinters.similarreplacer;

import com.google.common.base.Charsets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;

import java.io.*;
import java.util.*;

public class DynamicTemplateExecuter {

    public static void main(String[] args) throws IOException {

        String inputFileName = args[0];
        String outputFile = args[1];

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonReader reader = new JsonReader(new BufferedReader(
                new InputStreamReader(ClassLoader.getSystemResource(inputFileName).openStream())));
        List<String> corpus = gson.fromJson(reader, List.class);

        List<String> output = new ArrayList<>();
        for (int i = 0; i < corpus.size(); i++) {
            System.out.println(i + ": " + corpus.get(i));
            Optional<String> generated = Optional.empty();
            int trial = 0;
            while (!generated.isPresent() && trial <= 100) {
                TorfsSimilarWordReplacer repl = TorfsSimilarWordReplacer.create(Collections.singletonList(corpus.get(i)), corpus);
                System.out.println(i + " replacement : " + repl.get());
                generated = repl.generate();
                trial += 1;
                if (generated.isPresent() && corpus.contains(generated.get()) && trial <= 100) {
                    generated = Optional.empty();
                }
            }
            output.add(generated.get());

        }
        System.out.println("Output: " + output);
        try (FileWriter writer = new FileWriter(outputFile, Charsets.UTF_8)) {
            gson.toJson(output, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
