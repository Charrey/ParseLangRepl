package parselang.parser.exceptions;

public class ParseErrorException extends Exception {

    public ParseErrorException(String originalString, int index) {
        super("No alternative at index " + findRowColumn(originalString, index) + " at " + whichCharacter(originalString, index));
    }

    public ParseErrorException() {
    }

    private static String findRowColumn(String originalString, int index) {
        int row = 1;
        int otherchars = 1;
        for(int i = 0; i < index-1; i++) {
            if (originalString.charAt(i) == '\n') {
                row++;
                otherchars = 1;
            } else {
                otherchars++;
            }
        }
        return "(" + row + ":" + otherchars + ")";
    }

    private static String whichCharacter(String originalString, int index) {
        if (originalString.isEmpty()) {
            return "start of input";
        } else if (index == originalString.length()) {
            return "end of input";
        } else {
            if (index == originalString.length() + 1) {
                return "EOF";
            }
            return index == -1 ? "-1" : '\'' + toStringChar(originalString.charAt(index)) + '\'';
        }
    }

    private static String toStringChar(char mychar) {
        switch (mychar) {
            case '\t':
                return "\\t";
            case '\r':
                return "\\r";
            case '\n':
                return "\\n";
            default:
                return String.valueOf(mychar);
        }
    }

}
