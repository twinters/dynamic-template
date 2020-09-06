package be.thomaswinters.similarreplacer;

import be.thomaswinters.generator.generators.IGenerator;
import be.thomaswinters.markov.model.data.bags.Bag;
import be.thomaswinters.random.Picker;
import be.thomaswinters.replacement.Replacer;
import be.thomaswinters.replacement.Replacers;
import be.thomaswinters.wordcounter.WordCounter;
import org.languagetool.AnalyzedTokenReadings;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class TorfsSimilarWordReplacer extends SimilarWordReplacer implements IGenerator<String> {

    private final List<String> templateBases;
    private final WordCounter wc;

    public TorfsSimilarWordReplacer(List<String> dynamicTemplateBases, WordCounter wc) {
        this.templateBases = dynamicTemplateBases;
        this.wc = wc;
    }


    private static final double MIN_QUARTILE_FOR_REPLACEMENT = 0.62;

    /*-********************************************-*
     *  Construction
     *-********************************************-*/
    public static TorfsSimilarWordReplacer create(List<String> dynamicTemplateBases, List<String> contextCorpus) throws IOException {
        List<String> allLines = new ArrayList<String>(dynamicTemplateBases);
        allLines.addAll(contextCorpus);

        WordCounter wc = new WordCounter(allLines);

        int numberOfContextLines = 2;
        int numberOfDynamicTemplateAsContextLines = 1;
        boolean consequtiveContextLines = true;

        List<String> contextLines;
        if (consequtiveContextLines) {
            contextLines = Picker.pickConsequtiveIndices(numberOfContextLines, contextCorpus.size()).stream()
                    .map(contextCorpus::get).collect(Collectors.toList());
        } else {
            contextLines = Picker.pickRandomUniqueIndices(numberOfContextLines, contextCorpus.size()).stream()
                    .map(contextCorpus::get).collect(Collectors.toList());
        }
        List<String> dynamicTemplateContext = Picker.pickRandomUniqueIndices(numberOfDynamicTemplateAsContextLines, dynamicTemplateBases.size()).stream()
                .map(dynamicTemplateBases::get).collect(Collectors.toList());

        TorfsSimilarWordReplacer wordReplacer = new TorfsSimilarWordReplacer(dynamicTemplateBases, wc);
        wordReplacer.addContextWords(contextLines);
        wordReplacer.addContextWords(dynamicTemplateContext);

        return wordReplacer;
    }

    /*-********************************************-*
     *  GENERATOR
     *-********************************************-*/
    @Override
    public Optional<String> generate() {
        String randomDynamicTemplate = templateBases.get(RANDOM.nextInt(templateBases.size()));
        List<Replacer> replacers = calculatePossibleReplacements(randomDynamicTemplate);
        List<Replacer> chosenReplacers = pickReplacers(calculateMinNumberOfReplacements(randomDynamicTemplate), wc.getQuartileCount(MIN_QUARTILE_FOR_REPLACEMENT), replacers);

        String result = new Replacers(chosenReplacers).replace(randomDynamicTemplate);

//        System.out.println("\n\nTOTAL:\nFrom: " + randomDynamicTemplate + "\nTo:   " + result);
        return Optional.of(result);
    }

    private int calculateMinNumberOfReplacements(String randomTweet) {
        return randomTweet.length() / 25;
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

    @Override
    public String pickReplacement(String replacement, Bag<String> bag) {
        Comparator<String> comp = new ClosestWordCountComparator(replacement);

        return bag.toMultiset().stream().min(comp).get();
    }


    @Override
    public Optional<Replacer> createReplacer(AnalyzedTokenReadings token, Bag<String> replacePossibilities) {
        // Null check
        if (token == null || token.getToken().length() == 0) {
            return Optional.empty();
        }
        // Check if name:
        if (getTags(token).stream().allMatch(tag -> tag.startsWith("PN"))) {
            return Optional.empty();
        }

        return super.createReplacer(token, replacePossibilities);
    }

}
