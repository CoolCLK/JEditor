package coolclk.jeditor.api.javafx;

import javafx.beans.NamedArg;
import javafx.concurrent.Task;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CodeArea extends org.fxmisc.richtext.CodeArea {
    private final static List<ExecutorService> executorPool = new ArrayList<>();


    public CodeArea() {
        this("");
    }

    public CodeArea(@NamedArg("text") String text) {
        executor = Executors.newSingleThreadScheduledExecutor();
        executorPool.add(executor);
        setParagraphGraphicFactory(LineNumberFactory.get(this));
        this.multiPlainChanges()
                .successionEnds(Duration.ofMillis(100))
                .retainLatestUntilLater(executor)
                .supplyTask(this::computeHighlightingAsync)
                .awaitLatest(this.multiPlainChanges())
                .filterMap(t -> {
                    if (t.isSuccess()) {
                        return Optional.of(t.get());
                    } else {
                        t.getFailure().printStackTrace(System.err);
                        return Optional.empty();
                    }
                })
                .subscribe(this::applyHighlighting);
        this.textProperty().addListener(observable -> {
            if (!this.isTextChanged()) {
                this.textChanged = !Objects.equals(this.lastChangedText, this.getText());
                this.lastChangedText = this.getText();
            }
        });
        this.addEventFilter(KeyEvent.KEY_PRESSED, _event -> {
            if (this.isTextChanged()) {
                this.save(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN).match(_event));
            }
        });

        appendText(text);
        getUndoManager().forgetHistory();
        getUndoManager().mark();

        selectRange(0, 0);
    }

    /**
     * 可继承的保存动作
     * @param save 是否保存, 作为标记使用
     */
    public void save(boolean save) {
        this.textChanged = false;
    }

    public static void stopExecutors() {
        executorPool.forEach(ExecutorService::shutdown);
        executorPool.clear();
    }

    private String lastChangedText = this.getText();
    private boolean textChanged = false;
    public boolean isTextChanged() {
        return this.textChanged;
    }

    // From RichTextFX examples

    private static final String[] KEYWORDS = new String[] {
            "abstract", "assert", "boolean", "break", "byte",
            "case", "catch", "char", "class", "const",
            "continue", "default", "do", "double", "else",
            "enum", "extends", "final", "finally", "float",
            "for", "goto", "if", "implements", "import",
            "instanceof", "int", "interface", "long", "native",
            "new", "package", "private", "protected", "public",
            "return", "short", "static", "strictfp", "super",
            "switch", "synchronized", "this", "throw", "throws",
            "transient", "try", "void", "volatile", "while"
    };

    private static final String KEYWORD_PATTERN = "\\b(" + String.join("|", KEYWORDS) + ")\\b";
    private static final String PAREN_PATTERN = "\\(|\\)";
    private static final String BRACE_PATTERN = "\\{|\\}";
    private static final String BRACKET_PATTERN = "\\[|\\]";
    private static final String SEMICOLON_PATTERN = "\\;";
    private static final String STRING_PATTERN = "\"([^\"\\\\]|\\\\.)*\"";
    private static final String COMMENT_PATTERN = "//[^\n]*" + "|" + "/\\*(.|\\R)*?\\*/";

    private static final Pattern PATTERN = Pattern.compile(
            "(?<KEYWORD>" + KEYWORD_PATTERN + ")"
                    + "|(?<PAREN>" + PAREN_PATTERN + ")"
                    + "|(?<BRACE>" + BRACE_PATTERN + ")"
                    + "|(?<BRACKET>" + BRACKET_PATTERN + ")"
                    + "|(?<SEMICOLON>" + SEMICOLON_PATTERN + ")"
                    + "|(?<STRING>" + STRING_PATTERN + ")"
                    + "|(?<COMMENT>" + COMMENT_PATTERN + ")"
    );

    private final ExecutorService executor;

    private Task<StyleSpans<Collection<String>>> computeHighlightingAsync() {
        String text = this.getText();
        Task<StyleSpans<Collection<String>>> task = new Task<StyleSpans<Collection<String>>>() {
            @Override
            protected StyleSpans<Collection<String>> call() {
                return computeHighlighting(text);
            }
        };
        executor.execute(task);
        return task;
    }

    private void applyHighlighting(StyleSpans<Collection<String>> highlighting) {
        this.setStyleSpans(0, highlighting);
    }

    private static StyleSpans<Collection<String>> computeHighlighting(String text) {
        Matcher matcher = PATTERN.matcher(text);
        int lastKwEnd = 0;
        StyleSpansBuilder<Collection<String>> spansBuilder
                = new StyleSpansBuilder<>();
        while (matcher.find()) {
            String styleClass = getHighlightStyleClass(matcher);
            spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);
            spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
            lastKwEnd = matcher.end();
        }
        spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
        return spansBuilder.create();
    }

    private static String getHighlightStyleClass(Matcher matcher) {
        return matcher.group("KEYWORD") != null ? "highlight-keyword" :
               matcher.group("PAREN") != null ? "highlight-paren" :
               matcher.group("BRACE") != null ? "highlight-brace" :
               matcher.group("BRACKET") != null ? "highlight-bracket" :
               matcher.group("SEMICOLON") != null ? "highlight-semicolon" :
               matcher.group("STRING") != null ? "highlight-string" :
               matcher.group("COMMENT") != null ? "highlight-comment" :
               null;
    }
}
