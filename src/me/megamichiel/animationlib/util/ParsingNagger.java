package me.megamichiel.animationlib.util;

import me.megamichiel.animationlib.Nagger;
import me.megamichiel.animationlib.placeholder.ctx.ParsingContext;

public interface ParsingNagger extends Nagger {

    static ParsingNagger of(Nagger nagger, ParsingContext ctx) {
        return new ParsingNagger() {
            @Override
            public ParsingContext context() {
                return ctx;
            }

            @Override
            public void nag(String message) {
                nagger.nag(message);
            }

            @Override
            public void nag(Throwable throwable) {
                nagger.nag(throwable);
            }
        };
    }

    ParsingContext context();
}
