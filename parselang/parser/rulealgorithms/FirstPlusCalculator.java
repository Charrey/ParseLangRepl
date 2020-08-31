package parselang.parser.rulealgorithms;

import parselang.parser.data.Node;
import parselang.parser.data.NonTerminal;
import parselang.parser.data.ParseRule;
import parselang.util.TimedClass;

import java.util.*;

public abstract class FirstPlusCalculator extends TimedClass {

    public abstract void computeFirstPlus(Map<NonTerminal, Map<Character, TreeSet<ParseRule>>> init, Map<NonTerminal, List<ParseRule>> rules, Map<Node, Set<Character>> first, Map<Node, Set<Character>> follow, Collection<NonTerminal> nonTerminals);

}
