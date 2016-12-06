package me.megamichiel.animationlib.util;

public class StringReader {

    private final String source;
    private final int count;
    private int index = 0;

    public StringReader(String source) {
        count = (this.source = source).length();
    }

    public int available() {
        return count - index;
    }

    public boolean isReadable() {
        return index < count;
    }

    public char readChar() {
        return source.charAt(index++);
    }

    public char peekChar() {
        return source.charAt(index);
    }

    public void skip(int i) {
        index = Math.min(index + i, count);
    }

    public void skipChar() {
        if (index < count) index++;
    }

    public void unread(int count) {
        index = Math.max(index - count, 0);
    }

    public void unreadChar() {
        if (index > 0) index--;
    }

    public char[] readChars(int count) {
        char[] c = new char[count];
        for (int i = 0; i < c.length; i++) c[i] = readChar();
        return c;
    }

    public String readString(int length) {
        return new String(readChars(length));
    }

    public void skipWhitespace() {
        while (index < count) {
            char c = source.charAt(index);
            if (c == ' ' || c == '\t') index++;
            else break;
        }
    }

    public int index() {
        return index - 1;
    }

    public String source() {
        return source;
    }
}
