package parselang.interpreter;

import parselang.parser.data.ASTElem;

import java.util.Map;

public class Context {
    public final Map<String, ASTElem> variableAssignments;

    public Context(Map<String, ASTElem> variableAssignments) {
        this.variableAssignments = variableAssignments;
    }

}
