package info.kgeorgiy.ja.kasatov.arrayset;
import java.util.*;


public class ArraySet<T> extends AbstractSet<T> implements NavigableSet<T> {

    private final List<T> arrayList;
    private final Comparator<? super T> comparator;
    private final boolean naturalOrder;


    public ArraySet() {
        this(null, null, InitType.ORDINARY);
    }

    public ArraySet(Collection<? extends T> collection) {
        this(collection, null, InitType.ORDINARY);

    }

    public ArraySet(Collection<? extends T> collection, Comparator<? super T> comparator) {
        this(collection, comparator, InitType.ORDINARY);
    }

    @SuppressWarnings("unchecked")
    private Comparator<T> makeComparator() {
        return (o1, o2) -> ((Comparable<T>) o1).compareTo(o2);
    }

    enum InitType {
        ORDINARY,
        FROM_SUBSET,
        FROM_SORTED,
    }

    @SuppressWarnings("unchecked")
    private ArraySet(Collection<? extends T> collection, Comparator<? super T> comparator, InitType initType) {
        if (comparator == null) {
            naturalOrder = true;
            this.comparator = makeComparator();
        } else {
            naturalOrder = false;
            this.comparator = comparator;
        }

        if (collection == null) {
            arrayList = Collections.emptyList();
        } else {
            if (initType == InitType.FROM_SUBSET) {
                arrayList = (List<T>) collection;
            } else {
                if (initType == InitType.ORDINARY) {
                    TreeSet<T> treeSet = new TreeSet<>(comparator);
                    treeSet.addAll(collection);
                    collection = treeSet;
                } // else: Collection is already sorted
                arrayList = new ArrayList<>(collection);
            }
        }
    }

    public int getIndex(T t, boolean least, boolean inclusive) {
        int index = Collections.binarySearch(arrayList, t, comparator);
        if (index < 0) {
            index = -(index + 1);
            if (!least) {
                index--;
            }
        } else {
            if (!inclusive && comparator.compare(arrayList.get(index), t) == 0) {
                if (least) {
                    index++;
                } else {
                    index--;
                }
            }
        }
        return index;
    }

    @Override
    public T lower(T t) {
        int index = getIndex(t, false, false);
        return (index < 0) ? null : arrayList.get(index);
    }

    @Override
    public T floor(T t) {
        int index = getIndex(t, false, true);
        return (index < 0) ? null : arrayList.get(index);
    }

    @Override
    public T ceiling(T t) {
        int index = getIndex(t, true, true);
        return (index >= arrayList.size()) ? null : arrayList.get(index);
    }

    @Override
    public T higher(T t) {
        int index = getIndex(t, true, false);
        return (index >= arrayList.size()) ? null : arrayList.get(index);
    }

    @Override
    public T pollFirst() {
        throw new UnsupportedOperationException();
    }

    @Override
    public T pollLast() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int size() {
        return arrayList.size();
    }

    @Override
    public boolean isEmpty() {
        return arrayList.isEmpty();
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean contains(Object o) {
        T element = (T)  Objects.requireNonNull(o);
        int index = getIndex(element, false, true);
        return index >= 0 && comparator.compare(arrayList.get(index), element) == 0;
    }

    @Override
    public Iterator<T> iterator() {
        return Collections.unmodifiableList(arrayList).iterator();
    }

    @Override
    public Object[] toArray() {
        return arrayList.toArray();
    }

    @Override
    public <T1> T1[] toArray(T1[] a) {
        return arrayList.toArray(a);
    }

    @Override
    public boolean add(T t) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return c.stream().allMatch(this::contains);
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public NavigableSet<T> descendingSet() {
        return new ArraySet<>(arrayList, comparator.reversed());
    }

    @Override
    public Iterator<T> descendingIterator() {
        ArrayList<T> list = new ArrayList<>(arrayList);
        Collections.reverse(list);
        return list.iterator();
    }

    @Override
    public NavigableSet<T> subSet(T fromElement, boolean fromInclusive, T toElement, boolean toInclusive) {
        if (comparator.compare(fromElement, toElement) > 0) throw new IllegalArgumentException();
        return subSetByRange(
            getIndex(fromElement, true, fromInclusive),
            getIndex(toElement, false, toInclusive) + 1
        );
    }

    @Override
    public NavigableSet<T> headSet(T toElement, boolean inclusive) {
        int index = getIndex(toElement, false, inclusive);
        return subSetByRange(0, index + 1);
    }

    @Override
    public NavigableSet<T> tailSet(T fromElement, boolean inclusive) {
        int index = getIndex(fromElement, true, inclusive);
        return subSetByRange(index, arrayList.size());
    }

    @Override
    public Comparator<? super T> comparator() {
        return (naturalOrder) ? null : comparator;
    }

    private ArraySet<T> subSetByRange(int start, int end) {
        if (end < start) {
            return new ArraySet<>(null, comparator(), InitType.ORDINARY);
        }
        return new ArraySet<>(arrayList.subList(start, end), comparator(), InitType.FROM_SUBSET);
    }

    @Override
    public SortedSet<T> subSet(T fromElement, T toElement) {
        return subSet(fromElement, true, toElement, false);
    }

    @Override
    public SortedSet<T> headSet(T toElement) {
        return headSet(toElement, false);
    }

    @Override
    public SortedSet<T> tailSet(T fromElement) {
        return tailSet(fromElement, true);
    }

    @Override
    public T first() {
        if (arrayList.isEmpty()) throw new NoSuchElementException();
        return arrayList.get(0);
    }

    @Override
    public T last() {
        if (arrayList.isEmpty()) throw new NoSuchElementException();
        return arrayList.get(arrayList.size() - 1);
    }
}
