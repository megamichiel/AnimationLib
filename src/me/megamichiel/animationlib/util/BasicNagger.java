package me.megamichiel.animationlib.util;

import me.megamichiel.animationlib.Nagger;

public interface BasicNagger extends Nagger {

    @Override
    default void nag(Throwable throwable) {
        nag(throwable.toString());
    }
}
