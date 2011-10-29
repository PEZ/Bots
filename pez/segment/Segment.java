package pez.segment;

import pez.TuningFactor;

import java.util.*;

public class Segment {
    protected static final double VERY_CLOSE_UPPER = 300;
    protected static final double CLOSE_UPPER = 475;
    protected static final double FAR_LOWER = 575;
    protected static final double VERY_FAR_LOWER = 625;
    
    protected static final double LEFT_UPPER = -0.5;
    protected static final double RIGHT_LOWER = 0.5;

    private String m_segmentSuffix = null;
    
    private Segment(String segmentSuffix) {
        m_segmentSuffix = segmentSuffix;
    }
    
    public static Segment create(double distance, double bearingDelta) {
        String distanceKey = distanceKey(distance);
        String lateralKey = lateralKey(bearingDelta);
        return new Segment(distanceKey + lateralKey);
    }
    
    public String getFactorKey(String prefix) {
        return prefix + getSegmentSuffix();
    }
    
    public String getSegmentSuffix() {
        return m_segmentSuffix;
    }
    
    public static void connectSegments(Map factors, String prefix) {
        connect(factors, prefix + "VeryCloseLeft", prefix + "CloseLeft");
        connect(factors, prefix + "VeryClose", prefix + "Close");
        connect(factors, prefix + "VeryCloseRight", prefix + "CloseRight");

        connect(factors, prefix + "CloseLeft", prefix + "NormalLeft");
        connect(factors, prefix + "Close", prefix + "Normal");
        connect(factors, prefix + "CloseRight", prefix + "NormalRight");

        connect(factors, prefix + "FarLeft", prefix + "VeryFarLeft");
        connect(factors, prefix + "Far", prefix + "VeryFar");
        connect(factors, prefix + "FarRight", prefix + "VeryFarRight");
    }

    private static String distanceKey(double distance) {
        if (distance < VERY_CLOSE_UPPER) {
            return "VeryClose";
        }
        else if (distance < CLOSE_UPPER) {
            return "Close";
        }
        else if (distance > VERY_FAR_LOWER) {
            return "VeryFar";
        }
        else if (distance > FAR_LOWER) {
            return "Far";
        }
        else {
            return "Normal";
        }
    }

    private static String lateralKey(double bearingDelta) {
        if (bearingDelta < LEFT_UPPER ) {
            return "Left";
        }
        else if (bearingDelta > RIGHT_LOWER ) {
            return "Right";
        }
        else {
            return "";
        }
    }

    private static void connect(Map factors, String factor1, String factor2) {
        ((TuningFactor)factors.get(factor1)).connectNeighbour((TuningFactor)factors.get(factor2));
    }
}
