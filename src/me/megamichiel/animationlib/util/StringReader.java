package me.megamichiel.animationlib.util;

import java.util.Arrays;

public class StringReader {

    private final String source;
    private final char[] chars;
    private final int count;
    private int index = 0;

    public StringReader(String source) {
        count = (chars = (this.source = source).toCharArray()).length;
    }

    public int available() {
        return count - index;
    }

    public boolean isReadable() {
        return index < count;
    }

    public char readChar() {
        return chars[index++];
    }

    public char peekChar() {
        return chars[index];
    }

    public void skip(int i) {
        index = Math.min(index + i, count);
    }

    public void skipChar() {
        if (index < count) {
            ++index;
        }
    }

    public void unread(int count) {
        index = Math.max(index - count, 0);
    }

    public void unreadChar() {
        if (index > 0) --index;
    }

    public char[] readChars(int count) {
        return Arrays.copyOfRange(chars, index, index += count);
    }

    public String readString(int length) {
        return source.substring(index, index += length);
    }

    public void skipWhitespace() {
        for (char c; index < count; ++index) {
            if ((c = chars[index]) != ' ' && c != '\t') {
                break;
            }
        }
    }

    public int index() {
        return index - 1;
    }

    public String source() {
        return source;
    }
}
