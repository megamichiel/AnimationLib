package me.megamichiel.animationlib.util;

public interface Subscription {

    static Subscription create(Runnable unsubscriber) {
        return new Subscription() {
            boolean unsubscribed = false;
            @Override
            public void unsubscribe() {
                unsubscriber.run();
                unsubscribed = true;
            }

            @Override
            public boolean isUnsubscribed() {
                return unsubscribed;
            }
        };
    }

    void unsubscribe();
    boolean isUnsubscribed();
}
