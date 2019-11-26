package ru.jchat.core.client;

import javafx.scene.control.Tab;
import javafx.scene.control.TextArea;

class DialogTab extends Tab {
    private TextArea textArea;

    DialogTab(String name) {
        super(name);
        super.setId(name);
        textArea = new TextArea();
        this.setClosable(!name.equals(Controller.GENERAL));
        this.setContent(textArea);
    }

    TextArea getTextArea() {
        return textArea;
    }
}
