package bot;

import java.util.LinkedHashSet;
import java.util.regex.Pattern;

/**
 * Обработка сообщения, не являющегося командой (т.е. обычного текста не начинающегося с "/")
 */
public class NonCommand {
    public Boolean checkValid(String text) {
        Pattern pattern = Pattern.compile("((,*[ ]*)*[0-9]+(,*[ ]*)*)+");
        //System.out.println(pattern.matcher(text).pattern());

        return pattern.matcher(text).matches();
    }

    public int[] getNumbers(String text) {
        text = text.replaceAll(" ", "");
        text = text.replaceAll("\\t", "");
        text = text.replaceAll("\\s", "");
        String removeDuplicates = removeDuplicatesCommas(text);

        if (removeDuplicates.endsWith(",")) {
            removeDuplicates = removeDuplicates.substring(0, removeDuplicates.length() - 1);
        }

        if (removeDuplicates.indexOf(",") == 0) {
            removeDuplicates = removeDuplicates.substring(1);
        }

        if (removeDuplicates.equals("")) {
            return new int[]{};
        }

        String[] words = removeDuplicates.split(",");

        LinkedHashSet<Integer> numbers = new LinkedHashSet<>();

        try {
            for (String word : words) {
                numbers.add(Integer.parseInt(word));
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return new int[]{};
        }

        return numbers.stream().mapToInt(Number::intValue).toArray();
    }

    public String removeDuplicatesCommas(String input) {
        StringBuilder result = new StringBuilder(String.valueOf(input.charAt(0)));
        for (int i = 1; i < input.length(); i++) {
            if (!String.valueOf(input.charAt(i)).equals(String.valueOf(input.charAt(i - 1))) ||
                    (String.valueOf(input.charAt(i)).equals(String.valueOf(input.charAt(i - 1)))
                            && !String.valueOf(input.charAt(i)).equals(","))) {
                result.append((input.charAt(i)));
            }
        }
        return result.toString();
    }
}
