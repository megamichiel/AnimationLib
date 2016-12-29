package me.megamichiel.animationlib.placeholder.ctx;

import me.megamichiel.animationlib.placeholder.IPlaceholder;

import java.text.NumberFormat;

public interface ParsingContext {

    static ParsingContext ofFormat(NumberFormat nf) {
        return new AbstractParsingContext(nf) {
            @Override
            public IPlaceholder<?> parse(String identifier) {
                return null;
            }
        };
    }

    IPlaceholder<?> parse(String identifier);

    default NumberFormat numberFormat() {
        return NumberFormat.getInstance();
    }

    abstract class AbstractParsingContext implements ParsingContext {

        private final NumberFormat nf;

        public AbstractParsingContext(NumberFormat nf) {
            this.nf = nf;
        }

        @Override
        public NumberFormat numberFormat() {
            return nf;
        }
    }
}
