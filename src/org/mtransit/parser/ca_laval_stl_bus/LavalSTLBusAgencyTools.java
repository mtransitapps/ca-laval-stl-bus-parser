package org.mtransit.parser.ca_laval_stl_bus;

import java.util.HashSet;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.Utils;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GSpec;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MDirectionType;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.CleanUtils;
import org.mtransit.parser.mt.data.MTrip;

// http://www.stl.laval.qc.ca/en/stl-synchro/developers/
// http://www.stl.laval.qc.ca/opendata/GTF_STL.zip
public class LavalSTLBusAgencyTools extends DefaultAgencyTools {

	public static void main(String[] args) {
		if (args == null || args.length == 0) {
			args = new String[3];
			args[0] = "input/gtfs.zip";
			args[1] = "../../mtransitapps/ca-laval-stl-bus-android/res/raw/";
			args[2] = ""; // files-prefix
		}
		new LavalSTLBusAgencyTools().start(args);
	}

	private HashSet<String> serviceIds;

	@Override
	public void start(String[] args) {
		System.out.printf("\nGenerating STL bus data...");
		long start = System.currentTimeMillis();
		this.serviceIds = extractUsefulServiceIds(args, this);
		super.start(args);
		System.out.printf("\nGenerating STL bus data... DONE in %s.\n", Utils.getPrettyDuration(System.currentTimeMillis() - start));
	}


	@Override
	public boolean excludeRoute(GRoute gRoute) {
		if (this.serviceIds != null) {
			for (String serviceId : this.serviceIds) {
				if (gRoute.route_id.startsWith(serviceId.substring(0, 6))) {
					return false; // keep
				}
			}
			return true; // exclude
		}
		return super.excludeRoute(gRoute);
	}

	@Override
	public Integer getAgencyRouteType() {
		return MAgency.ROUTE_TYPE_BUS;
	}

	@Override
	public boolean excludeTrip(GTrip gTrip) {
		if (this.serviceIds != null) {
			return excludeUselessTrip(gTrip, this.serviceIds);
		}
		return super.excludeTrip(gTrip);
	}

	@Override
	public boolean excludeStop(GStop gStop) {
		if (this.serviceIds != null) {
			for (String serviceId : this.serviceIds) {
				if (gStop.stop_id.startsWith(serviceId.substring(0, 6))) {
					return false; // keep
				}
			}
			return true; // exclude
		}
		return super.excludeStop(gStop);
	}

	@Override
	public boolean excludeCalendarDate(GCalendarDate gCalendarDates) {
		if (this.serviceIds != null) {
			return excludeUselessCalendarDate(gCalendarDates, this.serviceIds);
		}
		return super.excludeCalendarDate(gCalendarDates);
	}

	@Override
	public boolean excludeCalendar(GCalendar gCalendar) {
		if (this.serviceIds != null) {
			return excludeUselessCalendar(gCalendar, this.serviceIds);
		}
		return super.excludeCalendar(gCalendar);
	}

	@Override
	public long getRouteId(GRoute gRoute) {
		return Integer.valueOf(gRoute.route_short_name); // using route short name instead of route ID
	}

	private static final Pattern DIRECTION = Pattern.compile("(direction)", Pattern.CASE_INSENSITIVE);
	private static final String DIRECTION_REPLACEMENT = " ";

	private static final Pattern TERMINUS = Pattern.compile("(terminus)", Pattern.CASE_INSENSITIVE);
	private static final String TERMINUS_REPLACEMENT = " ";

	private static final Pattern METRO = Pattern.compile("(m[e|é]tro)", Pattern.CASE_INSENSITIVE);
	private static final String METRO_REPLACEMENT = " ";

	private static final Pattern GARE = Pattern.compile("(gare)", Pattern.CASE_INSENSITIVE);
	private static final String GARE_REPLACEMENT = " ";

	@Override
	public String getRouteLongName(GRoute gRoute) {
		return cleanRouteLongName(gRoute.route_long_name);
	}

	private String cleanRouteLongName(String result) {
		result = DIRECTION.matcher(result).replaceAll(DIRECTION_REPLACEMENT);
		result = METRO.matcher(result).replaceAll(METRO_REPLACEMENT);
		result = GARE.matcher(result).replaceAll(GARE_REPLACEMENT);
		result = TERMINUS.matcher(result).replaceAll(TERMINUS_REPLACEMENT);
		result = CleanUtils.SAINT.matcher(result).replaceAll(CleanUtils.SAINT_REPLACEMENT);
		return CleanUtils.cleanLabel(result);
	}

	private static final String AGENCY_COLOR = "0053A6";

	@Override
	public String getAgencyColor() {
		return AGENCY_COLOR;
	}

	@Override
	public String getRouteColor(GRoute gRoute) {
		return null; // source file colors are nuts!
	}

	@Override
	public int getStopId(GStop gStop) {
		return Integer.valueOf(gStop.stop_code); // use stop code instead of stop ID
	}

	@Override
	public void setTripHeadsign(MRoute mRoute, MTrip mTrip, GTrip gTrip, GSpec gtfs) {
		String directionIdString = gTrip.getRouteId().substring(gTrip.getRouteId().length() - 1);
		mTrip.setHeadsignDirection(MDirectionType.parse(directionIdString));
	}

	private static final Pattern REMOVE_STOP_CODE_STOP_NAME = Pattern.compile("\\[[0-9]{5}\\]");

	private static final Pattern START_WITH_FACE_A = Pattern.compile("^(face à )", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
	private static final Pattern START_WITH_FACE_AU = Pattern.compile("^(face au )", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
	private static final Pattern START_WITH_FACE = Pattern.compile("^(face )", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

	private static final Pattern SPACE_FACE_A = Pattern.compile("( face à )", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
	private static final Pattern SPACE_WITH_FACE_AU = Pattern.compile("( face au )", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
	private static final Pattern SPACE_WITH_FACE = Pattern.compile("( face )", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

	private static final Pattern[] START_WITH_FACES = new Pattern[] { START_WITH_FACE_A, START_WITH_FACE_AU, START_WITH_FACE };

	private static final Pattern[] SPACE_FACES = new Pattern[] { SPACE_FACE_A, SPACE_WITH_FACE_AU, SPACE_WITH_FACE };

	@Override
	public String cleanStopName(String gStopName) {
		String result = gStopName;
		result = REMOVE_STOP_CODE_STOP_NAME.matcher(result).replaceAll(StringUtils.EMPTY);
		result = CleanUtils.CLEAN_SLASHES.matcher(result).replaceAll(CleanUtils.CLEAN_SLASHES_REPLACEMENT);
		result = Utils.replaceAll(result, START_WITH_FACES, CleanUtils.SPACE);
		result = Utils.replaceAll(result, SPACE_FACES, CleanUtils.SPACE);
		return super.cleanStopNameFR(result);
	}
}
