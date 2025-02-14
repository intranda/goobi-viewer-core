package io.goobi.viewer.controller.sorting;

import java.text.Collator;
import java.util.Comparator;
import java.util.Optional;
import java.util.function.Function;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class AlphabeticHtmlComparator<T> implements Comparator<T> {

    private final int reverse;
    private final Collator col;
    private final Function<T, String> stringifier;

    public AlphabeticHtmlComparator(boolean asc, Function<T, String> stringifier) {
        super();
        this.reverse = asc ? 1 : -1;
        col = Collator.getInstance();
        col.setStrength(Collator.PRIMARY);
        this.stringifier = stringifier;
    }

    @Override
    public int compare(T v1, T v2) {
        String s1 = this.stringifier.apply(v1);
        String s2 = this.stringifier.apply(v2);
        Document d1 = Jsoup.parse(s1);
        Document d2 = Jsoup.parse(s2);
        String test = d1.text();
        String test2 = d2.text();
        return this.reverse * Optional.ofNullable(d1.text()).orElse("").compareTo(Optional.ofNullable(d2.text()).orElse(""));
    }

}
