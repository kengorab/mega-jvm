package mega.lang.collections;

public class Arrays {
    public static String toString(Object[] arr) {
        return java.util.Arrays.deepToString(arr);
    }

    public static String strArrayToString(String[] arr) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");

        for (int i = 0; i < arr.length; i++) {
            String s = arr[i];
            sb.append("\"");
            sb.append(s);
            sb.append("\"");
            if (i < arr.length - 1) {
                sb.append(", ");
            }
        }
        sb.append("]");
        return sb.toString();
    }
}
