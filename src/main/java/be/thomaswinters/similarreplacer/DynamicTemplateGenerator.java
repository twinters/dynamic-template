package be.thomaswinters.similarreplacer;

import be.thomaswinters.generator.generators.IGenerator;
import be.thomaswinters.markov.model.data.bags.Bag;
import be.thomaswinters.random.Picker;
import be.thomaswinters.replacement.Replacer;
import be.thomaswinters.replacement.Replacers;
import be.thomaswinters.wordcounter.WordCounter;
import com.google.common.base.Charsets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

public class DynamicTemplateGenerator implements IGenerator<String> {

    private final List<String> templateBases;
    private final List<String> contextCorpus;
    private final WordCounter wc;

    private final IReplacementPicker replacementPicker = new ClosestWordReplacementPicker();
    private final double minQuartileForReplacement;

    private static final double DEFAULT_MIN_QUARTILE_FOR_REPLACEMENT = 0.62;

    /*-********************************************-*
     *  Construction
     *-********************************************-*/
    public DynamicTemplateGenerator(List<String> dynamicTemplateBases,
                                    List<String> contextCorpus,
                                    WordCounter wc,
                                    double minQuartileForReplacement) {
        this.templateBases = dynamicTemplateBases;
        this.contextCorpus = contextCorpus;
        this.wc = wc;
        this.minQuartileForReplacement = minQuartileForReplacement;
    }

    public DynamicTemplateGenerator(List<String> dynamicTemplateBases, List<String> contextCorpus) {
        this(dynamicTemplateBases, contextCorpus,
                calculateWordCounter(dynamicTemplateBases, contextCorpus),
                DEFAULT_MIN_QUARTILE_FOR_REPLACEMENT);
    }

    protected static WordCounter calculateWordCounter(List<String> dynamicTemplateBases, List<String> contextCorpus) {
        // Calculate word counter
        List<String> allLines = new ArrayList<String>(dynamicTemplateBases);
        allLines.addAll(contextCorpus);

        return new WordCounter(allLines);
    }


    /*-********************************************-*
     *  GENERATOR
     *-********************************************-*/

    public Optional<String> generate(
            String dynamicTemplate,
            int numberOfContextLines,
            boolean consequtiveContextLines) {

        List<String> contextLines = pickContextLines(this.contextCorpus, numberOfContextLines, consequtiveContextLines);

        SimilarWordReplacer wordReplacer = new SimilarWordReplacer();
        wordReplacer.addContextWords(contextLines);

        List<Replacer> replacers = wordReplacer.calculatePossibleReplacements(dynamicTemplate, replacementPicker);
        List<Replacer> chosenReplacers = pickReplacers(calculateMinNumberOfReplacements(dynamicTemplate),
                wc.getQuartileCount(minQuartileForReplacement), replacers);

        String result = new Replacers(chosenReplacers).replace(dynamicTemplate);

        return Optional.of(result);
    }

    public Optional<String> generate(
            int numberOfContextLines,
            boolean consequtiveContextLines) throws IOException {

        String randomDynamicTemplate = Picker.pick(templateBases);
        return generate(randomDynamicTemplate, numberOfContextLines, consequtiveContextLines);
    }


    @Override
    public Optional<String> generate() {
        try {
            return this.generate(getDefaultNumberOfContextLines(), true);
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    /*-********************************************-*
     *  DECIDING THE CONTEXT
     *-********************************************-*/

    @NotNull
    protected List<String> pickContextLines(List<String> contextCorpus, int numberOfContextLines, boolean consequtiveContextLines) {
        if (consequtiveContextLines) {
            return Picker.pickConsequtiveIndices(numberOfContextLines, contextCorpus.size()).stream()
                    .map(contextCorpus::get).collect(Collectors.toList());
        } else {
            return Picker.pickRandomUniqueIndices(numberOfContextLines, contextCorpus.size()).stream()
                    .map(contextCorpus::get).collect(Collectors.toList());
        }
    }

    private int calculateMinNumberOfReplacements(String randomTweet) {
        return Math.max(1, randomTweet.length() / 25);
    }

    /**
     * @param minAmount        The minimum number of replacers to use
     * @param maxWordFrequency Maximum frequency of a word that is about to be replaced, such that common words still remain intact
     * @param replacers        The list of potential replacers
     * @return
     */
    private List<Replacer> pickReplacers(int minAmount, int maxWordFrequency, Collection<Replacer> replacers) {
        List<Replacer> sorted = new ArrayList<>(replacers);
        sorted.sort(new ReplacerQuartileComparator());

        List<Replacer> result = new ArrayList<>();

        for (Replacer replacer : sorted) {
            if (result.size() < minAmount) {
//                System.out.println("Adding to min amount:" + replacer + ", " + wc.getCount(replacer.getWord()) + " / "
//                        + maxWordFrequency);
                result.add(replacer);
            } else if (wc.getCount(replacer.getWord()) < maxWordFrequency) {
//                System.out.println("Adding quartile count:" + replacer + ", " + wc.getCount(replacer.getWord()) + " / "
//                        + maxWordFrequency);
                result.add(replacer);
            } else {
//                System.out.println("Not adding: " + replacer + ", " + wc.getCount(replacer.getWord()) + " / "
//                        + maxWordFrequency);
            }
        }
        return result;
    }

    /*-********************************************-*
     * COMPARATORS
     *-********************************************-*/
    private class ClosestWordCountComparator implements Comparator<String> {
        private final String baseWord;

        public ClosestWordCountComparator(String baseWord) {
            this.baseWord = baseWord;
        }

        @Override
        public int compare(String word1, String word2) {
            return Math.abs(wc.getCount(word1) - wc.getCount(baseWord))
                    - Math.abs(wc.getCount(word2) - wc.getCount(baseWord));
        }
    }

    private class ReplacerQuartileComparator implements Comparator<Replacer> {
        @Override
        public int compare(Replacer r1, Replacer r2) {
            return wc.getCount(r1.getWord()) - wc.getCount(r2.getWord());
        }
    }

    class ClosestWordReplacementPicker implements IReplacementPicker {

        @Override
        public String pickReplacement(String replacement, Bag<String> bag) {
            Comparator<String> comp = new ClosestWordCountComparator(replacement);

            return bag.toMultiset().stream().min(comp).get();
        }
    }

    /*-********************************************-*
     * Access for children that want to override stuff
     *-********************************************-*/

    public int getDefaultNumberOfContextLines() {
        return 1;
    }

    protected List<String> getTemplateBases() {
        return templateBases;
    }

}
