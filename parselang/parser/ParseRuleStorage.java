package parselang.parser;


import parselang.languages.Language;
import parselang.parser.data.*;
import parselang.parser.rulealgorithms.*;
import parselang.util.Pair;

import java.util.*;
import java.util.stream.Collectors;

public class ParseRuleStorage {



    private final Map<NonTerminal, List<ParseRule>> rules = new HashMap<>();
    private final Map<Node, Set<Character>> first = new HashMap<>();
    private final Map<Node, Set<Character>> follow = new HashMap<>();
    private final Map<NonTerminal, Map<Character, TreeSet<ParseRule>>> rulesPlus = new HashMap<>();
    private final Set<NonTerminal> allNonterminals = new HashSet<>();

    public final FirstCalculator firstCalc = new NaiveFirstCalculator();
    public final FollowCalculator followCalc = new NaiveFollowCalculator();
    public final FirstPlusCalculator firstPlusCalc = new NaiveFirstPlusCalculator();


    public void prepare(Language lang, NonTerminal toplevel) {
        setDefaults(lang);
        calculateFirst();
        calculateFollow(toplevel);
        calculateFirstPlus();
    }

    public void addCustomRules(Pair<ParseRule, Direction> inheritedRule, Pair<ParseRule, Direction> addedRule, NonTerminal toplevel) {
        addRule(inheritedRule.getKey(), inheritedRule.getValue());
        addRule(addedRule.getKey(), addedRule.getValue());
        calculateFirst();
        calculateFollow(toplevel);
        calculateFirstPlus();
    }

    private List<ParseRule> addRule(ParseRule rule, Direction dir) {
        List<ParseRule> rules = rule.convertStarNodes();
        addRules(rules, dir);
        return rules;
    }

    private void addMissingNonterminals(Collection<Node> nodes) {
        for (Node node : nodes) {
            if (node instanceof NonTerminal) {
                allNonterminals.add((NonTerminal) node);
                this.rules.putIfAbsent((NonTerminal) node, new LinkedList<>());
            } else if (node instanceof StarNode) {
                addMissingNonterminals(((StarNode) node).contents());
            } else if (node instanceof BoundNonTerminal) {
                addMissingNonterminals(Collections.singleton(((BoundNonTerminal) node).getContent()));
            }
        }
    }

    private void addRules(Collection<ParseRule> rules, Direction dir) {
        for (ParseRule rule : rules) {
            NonTerminal nonTerminal = rule.getLHS();
            allNonterminals.add(nonTerminal);
            this.rules.computeIfAbsent(nonTerminal, nonTerminal1 -> new LinkedList<>());
            addMissingNonterminals(rule.getRHS());
            switch (dir) {
                case LEFT:
                    this.rules.get(nonTerminal).add(0, rule);
                    break;
                case RIGHT:
                    this.rules.get(nonTerminal).add(rule);
            }
        }
    }


    public Collection<ParseRule> getByNonTerminal(Node nonTerminal, Character startsWith) {
        if (!(nonTerminal instanceof NonTerminal)) {
            return Collections.emptyList();
        }
        if (rulesPlus.containsKey(nonTerminal)) {
            if (rulesPlus.get(nonTerminal).containsKey(startsWith)) {
                return rulesPlus.get(nonTerminal).get(startsWith);
            }
            return rulesPlus.get(nonTerminal).getOrDefault(null, new TreeSet<>(Comparator.comparingInt(value -> rules.get(nonTerminal).indexOf(value))));
        } else {
            System.out.println("Warning! No such rule! => " + ((NonTerminal)nonTerminal).getName() + ", starts with: \"" + startsWith + "\"");
            return Collections.emptyList();
        }
    }

    public static Node ws() {
        return new StarNode(nonTerm("WhiteSpace"));
    }

    public static NonTerminal nonTerm(String name) {
        return new NonTerminal(name);
    }

    public static Terminal term(String name) {
        return new Terminal(name);
    }

    public static BoundNonTerminal bound(Node node, String name, boolean lazy) {
        return new BoundNonTerminal(node, name, lazy);
    }

    public static Node star(Node... content) {
        return new StarNode(content);
    }

    public static Node star(List<Node> content) {
        return new StarNode(content.toArray(new Node[0]));
    }

    private void setDefaults(Language language) {
        List<ParseRule> originalRules = language.getRules();
        for (ParseRule rule : originalRules) {
            addRule(rule, Direction.RIGHT);
        }
    }


    public String toString() {
        StringBuilder sb = new StringBuilder();
        List<NonTerminal> nonTerminalDomain = rules.keySet().stream().sorted(Comparator.comparing(NonTerminal::getName)).collect(Collectors.toList());
        for (NonTerminal i : nonTerminalDomain) {
            sb.append(i.getName()).append(" =\n");
            for (int p = 0; p < rules.get(i).size(); p++) {
                List<Node> rhs = rules.get(i).get(p).getRHS();
                sb.append("\t");
                for (int j = 0; j < rhs.size()-1; j++) {
                    sb.append(rhs.get(j)).append(" ");
                }
                if (!rhs.isEmpty()) {
                    sb.append(rhs.get(rhs.size() - 1));
                }
                if (p != rules.get(i).size() - 1) {
                    sb.append(" |");
                }
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    private Set<Terminal> getAllTerminals() {
        Set<Terminal> res = new HashSet<>();
        for (List<ParseRule> rulesForNonTerminal : rules.values()) {
            for (ParseRule rule : rulesForNonTerminal) {
                for (Node token : rule.getRHS()) {
                    if (token instanceof Terminal) {
                        res.add((Terminal) token);
                    }
                }
            }
        }
        return res;
    }

    public Set<NonTerminal> getAllNonTerminals() {
        return allNonterminals;
    }

    private void calculateFirst() {
        first.clear();
        firstCalc.updateFirst(first, rules, getAllTerminals(), getAllNonTerminals());
    }

    private void calculateFirstPlus() {
        rulesPlus.clear();
        firstPlusCalc.computeFirstPlus(rulesPlus, rules, first, follow, getAllNonTerminals());
    }

    private void calculateFollow(NonTerminal startSymbol) {
        follow.clear();
        followCalc.updateFollow(follow, startSymbol, first, rules, getAllNonTerminals());
    }

    final Set<ParseRule> parameterNameRules = new HashSet<>();

    public void addParameter(ParseRule ruleToAdd, NonTerminal toplevel) {
        parameterNameRules.addAll(addRule(ruleToAdd, Direction.RIGHT));
        calculateFirst();
        calculateFollow(toplevel);
        calculateFirstPlus();
    }

    private final Set<NonTerminal> registered = new HashSet<>();

    public void registerNonTerminal(NonTerminal nonTerminal, NonTerminal toplevel) {
        if (!registered.contains(nonTerminal)) {
            registered.add(nonTerminal);
            addRule(new ParseRule("RegisteredNonTerminal").addRhs(term(nonTerminal.getName())), Direction.LEFT);
            calculateFirst();
            calculateFollow(toplevel);
            calculateFirstPlus();
        }
    }

    public void removeParameters(NonTerminal toplevel) {
        parameterNameRules.forEach(x -> rules.get(x.getLHS()).remove(x));
        parameterNameRules.clear();
        calculateFirst();
        calculateFollow(toplevel);
        calculateFirstPlus();
    }
}
