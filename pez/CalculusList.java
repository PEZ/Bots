package pez;
import java.util.*;

// Calculate rolling averages and stuff
// $Id: CalculusList.java,v 1.4 2004/02/20 09:55:35 peter Exp $

class CalculusList {
    private LinkedList values;
    private int maxDepth;
    private double sum;

    public CalculusList(int maxDepth) {
        this.maxDepth = maxDepth;
        values = new LinkedList();
    }

    void addValue(double value) {
        values.addLast(new Double(value));
        sum += value;
        if (values.size() > maxDepth) {
            sum -= ((Double)values.getFirst()).doubleValue();
            values.removeFirst();
        }
    }

    void addDelta(double value) {
        if (values.size() == 0) {
            addValue(0.0);
        }
        else {
            addValue(value - ((Double)values.get(values.size() - 1)).doubleValue());
        }
    }

    double average() {
        if (values.size() == 0) {
            return 0;
        }
        return sum / (double)values.size();
    }
}
