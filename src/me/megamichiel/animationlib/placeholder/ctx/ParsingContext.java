package me.megamichiel.animationlib.placeholder.ctx;

import me.megamichiel.animationlib.placeholder.IPlaceholder;

import java.text.NumberFormat;

public interface ParsingContext {

    static ParsingContext ofFormat(NumberFormat nf) {
        return new AbstractParsingContext(null, nf) {
            @Override
            public IPlaceholder<?> parse(String identifier) {
                return null;
            }
        };
    }

    static ParsingContext ofFormat(ParsingContext ctx, NumberFormat nf) {
        return ctx == null ? ofFormat(nf) : new AbstractParsingContext(ctx.parent(), nf) {
            @Override
            public IPlaceholder<?> parse(String identifier) {
                return ctx.parse(identifier);
            }
        };
    }

    IPlaceholder<?> parse(String identifier);

    default ParsingContext parent() {
        return null;
    }

    default NumberFormat numberFormat() {
        return NumberFormat.getInstance();
    }

    abstract class AbstractParsingContext implements ParsingContext {

        private final ParsingContext parent;
        private final NumberFormat nf;

        public AbstractParsingContext(ParsingContext parent, NumberFormat nf) {
            this.parent = parent;
            this.nf = nf;
        }

        public AbstractParsingContext(NumberFormat nf) {
            this(null, nf);
        }

        @Override
        public ParsingContext parent() {
            return parent;
        }

        @Override
        public NumberFormat numberFormat() {
            return nf;
        }
    }
}
