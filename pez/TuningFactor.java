package pez;
import robocode.*;
import java.util.*;
import java.io.Serializable;

// TuningFactor helps with remembering successful values of tuning parameters for Marshmallow robots
// $Id: TuningFactor.java,v 1.31 2004/02/20 09:55:35 peter Exp $

//Todo: Non-linear constructor.
//      Factor in how much better a factor really is. (chi2, std dev etc).
public class TuningFactor implements MarshmallowConstants, Serializable {
    static final long serialVersionUID = 1;
    private double min;
    private double max;
    private int steps;
    private ArrayList items;
    private transient Item currentItem;
    private transient Object[] sortedItems;
    private transient RatioComparator ratioComparator;
    private transient ResultsComparator resultsComparator;
    private transient ResultsRatioComparator resultsRatioComparator;
    private transient UseCountComparator useCountComparator;
    private int resultsDepth = MC_FACTOR_RESULTS_DEPTH;
    private transient Set neighbours;

    public TuningFactor(double min, double max, double stepSize) {
        this.min = min;
        this.max = max;
        this.steps = (int)Math.floor(((max - min) / stepSize) + 1);
        this.items = new ArrayList();
        for (int i = 0; i < steps; i++) {
            items.add(new Item(min + i * stepSize));
        }
        items.trimToSize();
        init();
    }

    void init() {
        for (int i = 0; i < steps; i++) {
            ((Item)items.get(i)).init();
        }
        sortedItems = items.toArray();
        ratioComparator = new RatioComparator();
        resultsComparator = new ResultsComparator();
        resultsRatioComparator = new ResultsRatioComparator();
        useCountComparator = new UseCountComparator();
        currentItem = getRandomItem();
        neighbours = new HashSet();
    }

    public boolean equals(Object object) {
        if (object instanceof TuningFactor) {
            return (((TuningFactor)object).getItems() == this.getItems());
        }
        return false;
    }

    public void connectNeighbour(TuningFactor neighbour) {
        neighbours.add(neighbour);
        if (!neighbour.hasNeighbour(this)) {
            neighbour.connectNeighbour(this);
        }
    }

    boolean hasNeighbour(TuningFactor neighbour) {
        return neighbours.contains(neighbour);
    }

    ArrayList getItems() {
        return this.items;
    }

    double getLowestUseCount() {
        Arrays.sort(sortedItems, useCountComparator);
        return ((Item)sortedItems[0]).getUses();
    }

    double getHighestRatio() {
        Arrays.sort(sortedItems, ratioComparator);
        return ((Item)sortedItems[normalizeIndex(-1)]).ratio();
    }

    void selectByRatio(int index) {
        Arrays.sort(sortedItems, ratioComparator);
        currentItem = (Item)sortedItems[normalizeIndex(index)];
    }

    void select(int index) {
        currentItem = (Item)items.get(normalizeIndex(index));
    }

    Item getItem(double value) {
        Iterator iterator = items.iterator();
        while (iterator.hasNext()) {
            Item item = (Item)iterator.next();
            if (item.value == value) {
                return item;
            }
        }
        return null;
    }

    void select(double value) {
        Item item = getItem(value);
        if (item != null) {
            currentItem = item;
        }
    }

    void selectHighestResultsRatio(int index) {
        Arrays.sort(sortedItems, resultsRatioComparator);
        currentItem = (Item)sortedItems[normalizeIndex(-1 - index)];
    }

    void selectLowestResultsRatio(int index) {
        Arrays.sort(sortedItems, resultsRatioComparator);
        currentItem = (Item)sortedItems[normalizeIndex(index)];
    }

    void selectLowestResultsRatio() {
        Arrays.sort(sortedItems, resultsRatioComparator);
        currentItem = (Item)sortedItems[0];
    }

    void selectLowestRatio() {
        Arrays.sort(sortedItems, ratioComparator);
        currentItem = (Item)sortedItems[0];
    }

    void selectHighestRatio() {
        Arrays.sort(sortedItems, ratioComparator);
        currentItem = (Item)sortedItems[normalizeIndex(-1)];
    }

    void selectHighestResults() {
        Arrays.sort(sortedItems, resultsComparator);
        currentItem = (Item)sortedItems[normalizeIndex(-1)];
    }

    void selectLowestResults() {
        Arrays.sort(sortedItems, resultsComparator);
        currentItem = (Item)sortedItems[0];
    }

    void selectRandom() {
        currentItem = this.getRandomItem();
    }

    double getRatio() {
        return currentItem.ratio();
    }

    double getResultsRatio() {
        return currentItem.resultsRatio();
    }

    double getUses() {
        return currentItem.uses;
    }

    double getValue() {
        return currentItem.value;
    }

    void incUses() {
        currentItem.incUses();
    }

    void incTuning() {
        currentItem.incTuning();
    }

    void incUses(double v) {
        int index = items.indexOf(new Item(v));
        if (index >= 0) {
            ((Item)items.get(index)).incUses();
        }
    }

    void incTuning(double v) {
        int index = items.indexOf(new Item(v));
        if (index >= 0) {
            ((Item)items.get(index)).incTuning();
        }
    }

    void decrementResults() {
        decrementResults(currentItem.value);
    }

    void decrementResults(double v) {
        Iterator iterator = items.iterator();
        while (iterator.hasNext()) {
            Item item = (Item)iterator.next();
            item.addResult(item.value == v ? 0.0 : 1.0);
        }
    }

    void incrementResults() {
        incrementResults(currentItem.value);
    }

    void incrementResults(double v) {
        Iterator iterator = items.iterator();
        while (iterator.hasNext()) {
            Item item = (Item)iterator.next();
            item.addResult(item.value == v ? 1.0 : 0.0);
        }
    }

    void addResult(double result) {
        currentItem.addResult(result);
    }

    void addResult(double v, double result) {
        int index = items.indexOf(new Item(v));
        if (index >= 0) {
            ((Item)items.get(index)).addResult(result);
        }
    }

    private int normalizeIndex(int index) {
        if (index < 0) {
            index = Math.max(0, steps + index);
        }
        else {
            index = Math.min(index, steps - 1);
        }
        return index;
    }

    private Item getRandomItem() {
        int random = (int)Math.floor(Math.random() * steps);
        return (Item)items.get(random);
    }

    void printStats(String label) {
        System.out.println("  TuningFactor: " + label);
        Iterator iterator = items.iterator();
        while (iterator.hasNext()) {
            Item item = (Item)iterator.next();
            System.out.println("    " + item.value + " - " +
                item.results() + " - " + item.tuning + "/" + item.uses + "=" + Math.round(item.ratio() * 10000.0) / 100.0);
        }
    }

    class Item implements Serializable, MarshmallowConstants {
        static final long serialVersionUID = 1;
        private long uses;
        private long tuning;
        private double value;
        private transient double[] results;

        public Item(double value) {
            this.value = value;
            init();
        }

        void init() {
            results = new double[resultsDepth];
        }

        public boolean equals(Object object) {
            if (object instanceof Item) {
                return (((Item)object).getValue() == this.getValue());
            }
            return false;
        }

        double getValue() {
            return this.value;
        }

        double getUses() {
            return this.uses;
        }

        double getTuning() {
            return this.tuning;
        }

        void incUses() {
            this.uses++;
        }

        void incTuning() {
            this.tuning++;
        }

        void addResult(double result) {
            results[(int)(uses % resultsDepth)] = result;
        }

        double resultsRatio() {
            double ratio = ratio();
            double results = results();
            if (ratio > -1) {
                return results * 0.4 + ratio * 100;
            }
            else {
                return 1 + results;
            }
        }

        double results() {
            double result = 0;
            for (int i = 0; i < results.length; i++) {
                result += results[i];
            }
            return result;
        }

        double ratio() {
            if (uses < 50) {
                double sumUses = uses;
                double sumTuning = tuning;
                Iterator iterator = neighbours.iterator();
                while (iterator.hasNext()) {
                    TuningFactor factor = (TuningFactor)iterator.next();
                    sumUses += factor.getItem(value).getUses();
                    sumTuning += factor.getItem(value).getTuning();
                }
                if (sumUses > 0) {
                    return sumTuning / sumUses;
                }
                else {
                    return -1.0;
                }
            }
            else {
                return (double)tuning / (double)uses;
            }
        }
    }

    class RatioComparator implements Comparator {
        public int compare(Object a, Object b) {
            if (((Item)a).ratio() < ((Item)b).ratio()) return(-1);
            if (((Item)a).ratio() == ((Item)b).ratio()) return(0);
            return(1);
        }
    }

    class ResultsComparator implements Comparator {
        public int compare(Object a, Object b) {
            if (((Item)a).results() < ((Item)b).results()) return(-1);
            if (((Item)a).results() == ((Item)b).results()) return(0);
            return(1);
        }
    }

    class ResultsRatioComparator implements Comparator {
        public int compare(Object a, Object b) {
            if (((Item)a).resultsRatio() < ((Item)b).resultsRatio()) return(-1);
            if (((Item)a).resultsRatio() == ((Item)b).resultsRatio()) return(0);
            return(1);
        }
    }

    class UseCountComparator implements Comparator {
        public int compare(Object a, Object b) {
            if (((Item)a).getUses() < ((Item)b).getUses()) return(-1);
            if (((Item)a).getUses() == ((Item)b).getUses()) return(0);
            return(1);
        }
    }
}
