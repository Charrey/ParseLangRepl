package parselang.parser.parsers;


import parselang.parser.ParseResult;
import parselang.parser.ParseRuleStorage;
import parselang.parser.TreeFixer;
import parselang.parser.data.*;
import parselang.parser.exceptions.ParseErrorException;
import parselang.util.DeclarationTree;
import parselang.util.Pair;

import java.util.*;

import static parselang.parser.ParseRuleStorage.*;

public class RecursiveParser extends Parser{

    private int farthestParse;
    private final TreeFixer treeFixer = new TreeFixer();

    private final MaxSizeDoubleMap<Integer, Node, ParseResult> memo           = new MaxSizeDoubleMap<>(1000);

    @Override
    public synchronized ParseResult parse(String originalString, Node toParseTo, ParseRuleStorage storage, NonTerminal toplevel) throws ParseErrorException {
        start();
        farthestParse = 0;
        try {
            ParseResult res = parse(originalString, 0, toParseTo, storage, toplevel);
            res.setTree((AST) treeFixer.fix(res.getTree()));
            if (res.getRemainingIndex() < originalString.length()) {
                throw new ParseErrorException(originalString, farthestParse);
            }
            stop();
            return res;
        } catch (ParseErrorException e) {
            stop();
            e.printStackTrace();
            throw new ParseErrorException(originalString, farthestParse);
        }
    }

    private ParseResult parse(String originalString, int notYetParsed, Node toParseTo, ParseRuleStorage storage, NonTerminal toplevel) throws ParseErrorException  {
        if (memo.contains(notYetParsed, toParseTo)) {
            return memo.get(notYetParsed, toParseTo);
        }
        if (originalString.length() < notYetParsed) {
            throw new ParseErrorException();
        }
        if (verbosity >= 1) {
            System.out.println(toParseTo + " ".repeat(100 - toParseTo.toString().length()) + originalString.substring(notYetParsed).replace("\n", "").replace("\r", ""));
        }
        if (toParseTo instanceof NonTerminal) {
            NonTerminal toParseToNT = (NonTerminal) toParseTo;
            storage.registerNonTerminal(toParseToNT, toplevel);
            Collection<ParseRule> rulesToTry = storage.getByNonTerminal(toParseToNT, notYetParsed == originalString.length() ? null : originalString.charAt(notYetParsed));
             for (ParseRule ruleToTry : rulesToTry) {
                try {
                    ParseResult res =  parseWithRule(originalString, notYetParsed, ruleToTry, storage, toplevel);
                    if (toParseTo.equals(nonTerm("Variable"))) {
                        addParameter(originalString, res.getTree(), storage, toplevel);
                    } else if (toParseTo.equals(nonTerm("NonTerminal"))) {
                        addNonTerminalName(originalString, res.getTree(), storage, toplevel);
                    } else if (toParseToNT.getName().equals("Declaration")) {
                        storage.removeParameters(toplevel);
                    }
                    memo.add(notYetParsed, toParseTo, res);
                    return res;
                } catch (ParseErrorException ignored) {
                }
            }
            throw new ParseErrorException();
        } else if (toParseTo instanceof Terminal) {
            return parseTerminal(originalString, notYetParsed, (Terminal) toParseTo);
        } else {
            throw new UnsupportedOperationException("Not yet implemented");
        }
    }


    private void addParameter(String originalString, AST tree, ParseRuleStorage storage, NonTerminal toplevel) {
        boolean lazy = ((AST)tree.getLastChild()).getChildren().size() == 1;
        ParseRule ruleToAdd;
        if (lazy) {
            ruleToAdd = new ParseRule("ParameterName").addRhs(term(originalString.substring(tree.getParsedFrom(), tree.getParsedTo() - 1)));
        } else {
            ruleToAdd = new ParseRule("ParameterName").addRhs(term(tree.subString(originalString)));
        }
        storage.addParameter(ruleToAdd, toplevel);
    }

    private void addNonTerminalName(String originalString, AST tree, ParseRuleStorage storage, NonTerminal toplevel) {
        storage.registerNonTerminal(new NonTerminal(tree.subString(originalString)), toplevel);
    }

    private void updateGrammar(String originalString, AST declaration, ParseRuleStorage storage, NonTerminal toplevel) {
        DeclarationTree declTree = new DeclarationTree(originalString, declaration);
        ParseRule inheritanceRule = new ParseRule(declTree.superNonTerminal).addRhs(nonTerm(declTree.name));
        ParseRule ruleToAdd2 = new ParseRule(declTree.name).addRhs(declTree.retrievedNodes.toArray(new Node[0]));
        storage.addCustomRules(new Pair<>(inheritanceRule, declTree.direction), new Pair<>(ruleToAdd2, Direction.RIGHT), toplevel);
    }



    private ParseResult parseTerminal(String originalString, int notYetParsed, Terminal toParseTo) throws ParseErrorException {
        if (memo.contains(notYetParsed, toParseTo)) {
            return memo.get(notYetParsed, toParseTo);
        }
        int size = toParseTo.getValue().length();
        if (originalString.length() <= notYetParsed || (originalString.charAt(notYetParsed) == toParseTo.getValue().charAt(0) && subStringStartsWith(originalString, notYetParsed, toParseTo.getValue()))) {
            AST tree = new AST(toParseTo);
            tree.setParsed(notYetParsed, notYetParsed + size);
            farthestParse = Math.max(farthestParse, notYetParsed + size);
            ParseResult res = new ParseResult(originalString, notYetParsed + size, tree);
            memo.add(notYetParsed, toParseTo, res);
            return res;
        } else {
            throw new ParseErrorException();
        }
    }

    private boolean subStringStartsWith(String originalString, int notYetParsed, String value) {
        //return originalString.substring(notYetParsed).startsWith(value);
        for (int i = 0; i < value.length(); i++) {
            if (originalString.charAt(i + notYetParsed) != value.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    private ParseResult parseWithRule(String originalString, int notYetParsed, ParseRule ruleToTry, ParseRuleStorage storage, NonTerminal toplevel) throws ParseErrorException {
        //System.out.println(recursionDepth.get(notYetParsed, ruleToTry));
        int newlyParsed = notYetParsed;
        AST ast = new AST(ruleToTry.getLHS());
        Deque<Node> toTry = new ArrayDeque<>(ruleToTry.getRHS());
        while (!toTry.isEmpty()) {
            Node node = toTry.pop();
            if (node instanceof NonTerminal && ((NonTerminal) node).getName().equals("DeclarationContent") && ruleToTry.getOrigin().equals(new ParseRule("Declaration").addRhs(nonTerm("NonTerminal"), ws(), nonTerm("GTorLT"), ws(), nonTerm("NonTerminal"), ws(), term("="), star(ws(), nonTerm("Token")), ws(), term("{"), ws(), nonTerm("DeclarationContent"), ws(), term("}")))) {
                updateGrammar(originalString, ast, storage, toplevel);
            }
            if (node instanceof NonTerminal || node instanceof Terminal) {
                ParseResult subResult = parse(originalString, newlyParsed, node, storage, toplevel);
                newlyParsed = subResult.getRemainingIndex();
                ast.addChild(subResult.getTree());
            } else if (node instanceof BoundNonTerminal) {
                toTry.push(((BoundNonTerminal) node).getContent());
                //throw new UnsupportedOperationException();
            }
        }
        ast.setParsed(notYetParsed, newlyParsed);
        ParseResult res = new ParseResult(originalString, newlyParsed, ast, ruleToTry);
        memo.add(notYetParsed, ruleToTry.getLHS(), res);
        return res;
    }


}
