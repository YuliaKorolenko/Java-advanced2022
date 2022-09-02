package info.kgeorgiy.ja.korolenko.arrayset;

import java.util.*;

public class ArraySet<T> extends AbstractSet<T> implements SortedSet<T> {
    private final List<T> elements;
    private final Comparator<T> comparator;

    public ArraySet(final ArraySet<T> elements) {
        this.comparator = elements.comparator;
        this.elements = elements.elements;
    }

    public ArraySet(final Collection<T> elements, final Comparator<T> comparator) {
        this.comparator = comparator;
        final TreeSet<T> set = new TreeSet<>(comparator);
        set.addAll(elements);
        this.elements = List.copyOf(set);
    }

    public ArraySet() {
        this(List.of(), null);
    }

    public ArraySet(final Collection<T> elements) {
        this(elements, null);
    }

    public ArraySet(final Comparator<T> comparator) {
        this(List.of(), comparator);
    }

    // :NOTE: удаление
    @Override
    public Iterator<T> iterator() {
        return elements.iterator();
    }

    @Override
    public int size() {
        return elements.size();
    }


    @Override
    public Comparator<T> comparator() {
        return this.comparator;
    }


    private int placeForInsert(final T element){
        final int number = Collections.binarySearch(this.elements, element, comparator);
        return number < 0 ? -number - 1 : number;
    }

    @Override
    public ArraySet<T> subSet(final T fromElement, final T toElement) {
        final int numberToElement = placeForInsert(fromElement);
        final int numberFromElement = placeForInsert(toElement);

        if (numberToElement >= numberFromElement) {
            throw new IllegalArgumentException();
        }
        return subSet(numberFromElement, numberToElement);
    }

    private ArraySet<T> subSet(final int from, final int to) {
        return new ArraySet<>(elements.subList(from, to), comparator);
    }

    @Override
    public ArraySet<T> headSet(final T toElement) {
        return subSet(0, placeForInsert(toElement));
    }


    @Override
    public ArraySet<T> tailSet(final T fromElement) {
        return subSet(placeForInsert(fromElement), this.elements.size());
    }

    private void nonEmpty() {
        if (elements.isEmpty()) throw new NoSuchElementException();
    }

    @Override
    public T first() {
        nonEmpty();
        return elements.get(0);
    }

    @Override
    public T last() {
        nonEmpty();
        return elements.get(elements.size() - 1);
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean contains(final Object sub) {
        return Collections.binarySearch(this.elements, (T) sub, comparator) >= 0;
    }
}
