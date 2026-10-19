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

    public double getValue() {
        return value;
    }
}
