package se.miun.dt175g.octi.client;

public class RewardValue implements Comparable<RewardValue> {
    private enum Type {LOSS, STEP, WIN};
    private final int depth;
    private final double value;
    private final Type type;

    public RewardValue(double value, int depth) {
        this.value = value;
        this.depth = depth;
        this.type = Type.STEP;
    }

    public RewardValue(int depth, boolean isWin) {
        this.value = 0.0;
        this.depth = depth;
        this.type = isWin ? Type.WIN : Type.LOSS;
    }

    public boolean isGreaterThan(RewardValue value) {
        if (value == null) return true;
        return compareTo(value) > 0;
    }

    public boolean isLessThan(RewardValue value) {
        if (value == null) return true;
        return compareTo(value) < 0;
    }

    public boolean isEqualTo(RewardValue value) {
        if (value == null) return false;
        return compareTo(value) == 0;
    }

    public int compareTo(RewardValue value) {
        if (type != value.type) {
            return type.compareTo(value.type);
        }
        if (type == Type.WIN) {
            return Integer.compare(depth, value.depth);
        }
        if (type == Type.LOSS) {
            return Integer.compare(value.depth, depth);
        }
        return Double.compare(this.value, value.value);
    }

    public static RewardValue max(RewardValue a, RewardValue b) {
        if (a == null && b == null) throw new IllegalArgumentException("Both values are null");
        if (a == null) return b;
        if (b == null) return a;
        return a.compareTo(b) > 0 ? a : b;
    }

    public static RewardValue min(RewardValue a, RewardValue b) {
        if (a == null && b == null) throw new IllegalArgumentException("Both values are null");
        if (a == null) return b;
        if (b == null) return a;
        return a.compareTo(b) < 0 ? a : b;
    }
}
