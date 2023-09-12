package coolclk.jeditor.api;

import javafx.beans.NamedArg;

import java.util.Arrays;
import java.util.List;

public class CodeArea extends org.fxmisc.richtext.CodeArea {
    public final static List<String> keyword = Arrays.asList("abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "continue", "default", "do", "double", "else", "enum", "extends", "final", "finally", "float", "for", "if", "implements", "import", "int", "interface", "instanceof", "long", "native", "new", "package", "private", "protected", "public", "return", "short", "static", "strictfp", "super", "switch", "synchronized", "this", "throw", "throws", "transient", "try", "void", "volatile", "while");
    public final static List<String> reserved = Arrays.asList("goto", "const");
    public final static List<String> literal = Arrays.asList("true", "false", "null");

    public CodeArea() {
        super();
        this.setOnKeyTyped(event -> {
            int index = 0;
            boolean stringConstant = false, characterConstant = false;
            StringBuilder word = new StringBuilder();
            for (Character character : this.getText().toCharArray()) {
                switch (character) {
                    case ';':
                    case '(':
                    case ')': {
                        word = new StringBuilder();
                        break;
                    }
                    case '"': {
                        stringConstant = !stringConstant;
                        break;
                    }
                    case '\'': {
                        characterConstant = !characterConstant;
                        break;
                    }
                    default: {
                        word.append(character);
                        break;
                    }
                }

                if (keyword.contains(word.toString()) || reserved.contains(word.toString()) || literal.contains(word.toString())) {
                    this.setStyleClass(index - word.length(), index, "-fx-font-color: #CF8E6D;");
                }

                index++;
            }
        });
    }

    public CodeArea(@NamedArg("text") String text) {
        this();

        appendText(text);
        getUndoManager().forgetHistory();
        getUndoManager().mark();

        selectRange(0, 0);
    }
}
