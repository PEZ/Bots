package pez;

import java.util.HashMap;
import pez.segment.Segment;

public class TuningFactorMap extends HashMap implements MarshmallowConstants {
    public static TuningFactorMap createDefault(double sectorAimWidth, double distanceStepWidth) {  
        TuningFactorMap map = new TuningFactorMap();
        map.put("virtualGunFactorVeryClose", new TuningFactor(0, MC_AIM_METHODS - 1, 1));
        map.put("virtualGunFactorVeryCloseLeft", new TuningFactor(0, MC_AIM_METHODS - 1, 1));
        map.put("virtualGunFactorVeryCloseRight", new TuningFactor(0, MC_AIM_METHODS - 1, 1));
        map.put("virtualGunFactorClose", new TuningFactor(0, MC_AIM_METHODS - 1, 1));
        map.put("virtualGunFactorCloseLeft", new TuningFactor(0, MC_AIM_METHODS - 1, 1));
        map.put("virtualGunFactorCloseRight", new TuningFactor(0, MC_AIM_METHODS - 1, 1));
        map.put("virtualGunFactorNormal", new TuningFactor(0, MC_AIM_METHODS - 1, 1));
        map.put("virtualGunFactorNormalLeft", new TuningFactor(0, MC_AIM_METHODS - 1, 1));
        map.put("virtualGunFactorNormalRight", new TuningFactor(0, MC_AIM_METHODS - 1, 1));
        map.put("virtualGunFactorFar", new TuningFactor(0, MC_AIM_METHODS - 1, 1));
        map.put("virtualGunFactorFarLeft", new TuningFactor(0, MC_AIM_METHODS - 1, 1));
        map.put("virtualGunFactorFarRight", new TuningFactor(0, MC_AIM_METHODS - 1, 1));
        map.put("virtualGunFactorVeryFar", new TuningFactor(0, MC_AIM_METHODS - 1, 1));
        map.put("virtualGunFactorVeryFarLeft", new TuningFactor(0, MC_AIM_METHODS - 1, 1));
        map.put("virtualGunFactorVeryFarRight", new TuningFactor(0, MC_AIM_METHODS - 1, 1));

        map.put("sectorAimFactorVeryClose", new TuningFactor(MC_SECTOR_AIM_MIN, MC_SECTOR_AIM_MAX, sectorAimWidth));
        map.put("sectorAimFactorVeryCloseLeft", new TuningFactor(MC_SECTOR_AIM_MIN, MC_SECTOR_AIM_MAX, sectorAimWidth));
        map.put("sectorAimFactorVeryCloseRight", new TuningFactor(MC_SECTOR_AIM_MIN, MC_SECTOR_AIM_MAX, sectorAimWidth));
        map.put("sectorAimFactorClose", new TuningFactor(MC_SECTOR_AIM_MIN, MC_SECTOR_AIM_MAX, sectorAimWidth));
        map.put("sectorAimFactorCloseLeft", new TuningFactor(MC_SECTOR_AIM_MIN, MC_SECTOR_AIM_MAX, sectorAimWidth));
        map.put("sectorAimFactorCloseRight", new TuningFactor(MC_SECTOR_AIM_MIN, MC_SECTOR_AIM_MAX, sectorAimWidth));
        map.put("sectorAimFactorNormal", new TuningFactor(MC_SECTOR_AIM_MIN, MC_SECTOR_AIM_MAX, sectorAimWidth));
        map.put("sectorAimFactorNormalLeft", new TuningFactor(MC_SECTOR_AIM_MIN, MC_SECTOR_AIM_MAX, sectorAimWidth));
        map.put("sectorAimFactorNormalRight", new TuningFactor(MC_SECTOR_AIM_MIN, MC_SECTOR_AIM_MAX, sectorAimWidth));
        map.put("sectorAimFactorFar", new TuningFactor(MC_SECTOR_AIM_MIN, MC_SECTOR_AIM_MAX, sectorAimWidth));
        map.put("sectorAimFactorFarLeft", new TuningFactor(MC_SECTOR_AIM_MIN, MC_SECTOR_AIM_MAX, sectorAimWidth));
        map.put("sectorAimFactorFarRight", new TuningFactor(MC_SECTOR_AIM_MIN, MC_SECTOR_AIM_MAX, sectorAimWidth));
        map.put("sectorAimFactorVeryFar", new TuningFactor(MC_SECTOR_AIM_MIN, MC_SECTOR_AIM_MAX, sectorAimWidth));
        map.put("sectorAimFactorVeryFarLeft", new TuningFactor(MC_SECTOR_AIM_MIN, MC_SECTOR_AIM_MAX, sectorAimWidth));
        map.put("sectorAimFactorVeryFarRight", new TuningFactor(MC_SECTOR_AIM_MIN, MC_SECTOR_AIM_MAX, sectorAimWidth));

        map.put("escapeAreaFactorVeryClose", new TuningFactor(0, MC_ESCAPE_AREA_SECTOR_MAX, 1));
        map.put("escapeAreaFactorVeryCloseLeft", new TuningFactor(0, MC_ESCAPE_AREA_SECTOR_MAX, 1));
        map.put("escapeAreaFactorVeryCloseRight", new TuningFactor(0, MC_ESCAPE_AREA_SECTOR_MAX, 1));
        map.put("escapeAreaFactorClose", new TuningFactor(0, MC_ESCAPE_AREA_SECTOR_MAX, 1));
        map.put("escapeAreaFactorCloseLeft", new TuningFactor(0, MC_ESCAPE_AREA_SECTOR_MAX, 1));
        map.put("escapeAreaFactorCloseRight", new TuningFactor(0, MC_ESCAPE_AREA_SECTOR_MAX, 1));
        map.put("escapeAreaFactorNormal", new TuningFactor(0, MC_ESCAPE_AREA_SECTOR_MAX, 1));
        map.put("escapeAreaFactorNormalLeft", new TuningFactor(0, MC_ESCAPE_AREA_SECTOR_MAX, 1));
        map.put("escapeAreaFactorNormalRight", new TuningFactor(0, MC_ESCAPE_AREA_SECTOR_MAX, 1));
        map.put("escapeAreaFactorFar", new TuningFactor(0, MC_ESCAPE_AREA_SECTOR_MAX, 1));
        map.put("escapeAreaFactorFarLeft", new TuningFactor(0, MC_ESCAPE_AREA_SECTOR_MAX, 1));
        map.put("escapeAreaFactorFarRight", new TuningFactor(0, MC_ESCAPE_AREA_SECTOR_MAX, 1));
        map.put("escapeAreaFactorVeryFar", new TuningFactor(0, MC_ESCAPE_AREA_SECTOR_MAX, 1));
        map.put("escapeAreaFactorVeryFarLeft", new TuningFactor(0, MC_ESCAPE_AREA_SECTOR_MAX, 1));
        map.put("escapeAreaFactorVeryFarRight", new TuningFactor(0, MC_ESCAPE_AREA_SECTOR_MAX, 1));
        
        return map;
    }   
    
    public TuningFactor getVirtualGunFactor(double distance, double bearingDelta) {
        return (TuningFactor)get(Segment.create(distance, bearingDelta).getFactorKey("virtualGunFactor") );
    }
    
    public TuningFactor getSectorAimFactor(double distance, double bearingDelta) {
        return (TuningFactor)get(Segment.create(distance, bearingDelta).getFactorKey("sectorAimFactor") );
    }
    
    public TuningFactor getEscapeAreaFactor(double distance, double bearingDelta) {
        return (TuningFactor)get(Segment.create(distance, bearingDelta).getFactorKey("escapeAreaFactor") );
    }
}
