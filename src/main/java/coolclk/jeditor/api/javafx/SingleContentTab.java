package coolclk.jeditor.api.javafx;

import javafx.scene.Node;
import javafx.scene.control.Tab;

public class SingleContentTab<T extends Node> extends Tab {
    public SingleContentTab() {
        this(null);
    }

    public SingleContentTab(String text) {
        this(text, null);
    }

    public SingleContentTab(String text, T content) {
        super(text, content);
    }

    @SuppressWarnings({ "unchecked" })
    public final T getSingleContent() {
        return (T) this.getContent();
    }
}
