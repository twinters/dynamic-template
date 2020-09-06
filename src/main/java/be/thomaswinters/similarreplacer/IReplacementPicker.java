package be.thomaswinters.similarreplacer;

import be.thomaswinters.markov.model.data.bags.Bag;

public interface IReplacementPicker {

    public String pickReplacement(String replacement, Bag<String> bag);
}
