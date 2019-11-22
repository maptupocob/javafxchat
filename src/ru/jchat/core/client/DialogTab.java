package ru.jchat.core.client;

import javafx.scene.control.Tab;
import javafx.scene.control.TextArea;

public class DialogTab extends Tab {
    TextArea textArea;

    public DialogTab(String name) {
        super(name);
        super.setId(name);
        textArea = new TextArea();
        this.setClosable(name.equals(Controller.GENERAL)? false : true);
        this.setContent(textArea);
    }

    public TextArea getTextArea() {
        return textArea;
    }
}
