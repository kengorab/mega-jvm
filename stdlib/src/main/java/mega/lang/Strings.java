package mega.lang;

public class Strings {
    public static String repeat(String target, int times) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < times; i++) {
            sb.append(target);
        }
        return sb.toString();
    }
}
