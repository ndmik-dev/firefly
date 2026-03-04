package ua.ndmik.bot.util;

import java.math.BigInteger;
import java.util.Comparator;
import java.util.Objects;

public final class GroupIdComparator implements Comparator<String> {

    public static final GroupIdComparator INSTANCE = new GroupIdComparator();

    private GroupIdComparator() {
    }

    @Override
    public int compare(String left, String right) {
        if (Objects.equals(left, right)) {
            return 0;
        }
        if (left == null) {
            return 1;
        }
        if (right == null) {
            return -1;
        }

        String[] leftParts = left.split("\\.", -1);
        String[] rightParts = right.split("\\.", -1);
        int partsToCompare = Math.min(leftParts.length, rightParts.length);

        for (int i = 0; i < partsToCompare; i++) {
            int comparison = comparePart(leftParts[i], rightParts[i]);
            if (comparison != 0) {
                return comparison;
            }
        }

        int byLength = Integer.compare(leftParts.length, rightParts.length);
        return byLength != 0
                ? byLength
                : left.compareTo(right);
    }

    private int comparePart(String leftPart, String rightPart) {
        boolean leftNumeric = isNumeric(leftPart);
        boolean rightNumeric = isNumeric(rightPart);

        if (leftNumeric && rightNumeric) {
            BigInteger leftNumber = new BigInteger(leftPart);
            BigInteger rightNumber = new BigInteger(rightPart);
            int byNumber = leftNumber.compareTo(rightNumber);
            return byNumber != 0
                    ? byNumber
                    : Integer.compare(leftPart.length(), rightPart.length());
        }
        if (leftNumeric != rightNumeric) {
            return leftNumeric ? -1 : 1;
        }
        return leftPart.compareTo(rightPart);
    }

    private boolean isNumeric(String value) {
        return !value.isEmpty() && value.chars().allMatch(Character::isDigit);
    }
}
