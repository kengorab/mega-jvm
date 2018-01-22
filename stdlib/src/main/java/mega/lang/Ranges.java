package mega.lang;

public class Ranges {
    public static Integer[] of(int start, int endNotInclusive) {
        int size = endNotInclusive - start;
        Integer[] range = new Integer[size];
        for (int i = 0; i < size; i++) {
            range[i] = start + i;
        }
        return range;
    }
}
