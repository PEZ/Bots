package pez;

// Constants for Marshmallow the Robot, by PEZ
// $Id: MarshmallowConstants.java,v 1.79 2004/02/20 09:55:35 peter Exp $
public interface MarshmallowConstants {
        static final double MC_FULL_TURN = 360.0;
	static final double MC_CLOSE = 130.0;
        static final int MC_RECENT_ENEMY = 16;
        static final int MC_RECENT_ENEMY_1V1 = 2;
        static final double MC_MAX_ENERGY = 1000;
        static final double MC_MAX_ROBOT_VELOCITY = 8.0;
        static final double MC_MAX_BULLET_VELOCITY = 19.7;
        static final double MC_MAX_BEARING_DELTA = 7;
        static final double MC_WALL_MARGIN = 39;
        static final int MC_RECORDING_SIZE = 5000;
        static final int MC_MATCH_PERIOD_LENGTH = 25;
        static final double MC_MATCH_DIFFERENCE_THRESHOLD = 0.01;
        static final int MC_AIM_STRAIGHT = 0;
        static final int MC_AIM_RANDOM_NARROW = 1;
        static final int MC_AIM_RANDOM_NORMAL = 2;
        static final int MC_AIM_OFFSET_FACTORED = 3;
        static final int MC_AIM_PATTERN_MATCH = 4;
        static final int MC_AIM_SECTOR_AIM = 5;
        static final int MC_AIM_ESCAPE_AREA_AIM = 6;
        static final int MC_AIM_ANTI_MIRROR = 7;
        static final int MC_AIM_ANGULAR = 8;
        static final int MC_AIM_ANGULAR_FACTORED = 9;
        static final int MC_AIM_METHODS = 8;
        static final int MC_SECTOR_AIM_MIN = -40;
        static final int MC_SECTOR_AIM_MAX = 40;
        static final int MC_SECTOR_AIMS = 10;
        static final int MC_ESCAPE_AREA_SECTOR_MAX = 11;
        static final int MC_FACTOR_RESULTS_DEPTH = 24;
        static final int MC_DISTANCE_MIN = 350;
        static final int MC_DISTANCE_MAX = 600;
        static final int MC_DISTANCES = 3;
        static final double MC_DISTANCE_HIT_TUNING_WEIGHT = 0.18;
        static final String MC_STATISTICS_FILE_POSTFIX_MELEE = "-Melee";
        static final String MC_STATISTICS_FILE_POSTFIX_1V1 = "-1v1";
        static final int MC_MOVEMENT_STRATEGY_DODGE = 0;
        static final int MC_MOVEMENT_STRATEGY_RANDOM = 1;
        static final double MC_WANTED_DISTANCE = 600;
}
