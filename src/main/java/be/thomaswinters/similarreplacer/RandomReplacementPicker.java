package be.thomaswinters.similarreplacer;

import be.thomaswinters.markov.model.data.bags.Bag;

import java.util.Random;

public class RandomReplacementPicker implements IReplacementPicker {
    protected static final Random RANDOM = new Random();

    /**
     * Picks a random replacement from the bag
     */
    public String pickReplacement(String replacement, Bag<String> bag) {
        return bag.get(RANDOM.nextInt(bag.getAmountOfElements()));
    }
}
