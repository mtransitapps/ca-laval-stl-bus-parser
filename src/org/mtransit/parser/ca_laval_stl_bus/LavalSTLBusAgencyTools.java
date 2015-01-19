package org.mtransit.parser.ca_laval_stl_bus;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.Utils;
import org.mtransit.parser.gtfs.GReader;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GSpec;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.mt.data.MDirectionType;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.mt.data.MSpec;
import org.mtransit.parser.mt.data.MTrip;

// http://www.stl.laval.qc.ca/en/stl-synchro/developers/
// http://www.stl.laval.qc.ca/opendata/GTF_STL.zip
public class LavalSTLBusAgencyTools extends DefaultAgencyTools {

	public static final String ROUTE_TYPE_FILTER = "3"; // bus only


	public static void main(String[] args) {
		if (args == null || args.length == 0) {
			args = new String[3];
			args[0] = "input/gtfs.zip";
			args[1] = "../../mtransitapps/ca-laval-stl-bus-android/res/raw/";
			args[2] = ""; // files-prefix
		}
		new LavalSTLBusAgencyTools().start(args);
	}

	private HashSet<String> startWithFilters;

	@Override
	public void start(String[] args) {
		System.out.printf("Generating STL bus data...\n");
		long start = System.currentTimeMillis();
		extractUsefulServiceIds(args);
		super.start(args);
		System.out.printf("Generating STL bus data... DONE in %s.\n", Utils.getPrettyDuration(System.currentTimeMillis() - start));
	}

	private void extractUsefulServiceIds(String[] args) {
		System.out.printf("Extracting useful service IDs...\n");
		GSpec gtfs = GReader.readGtfsZipFile(args[0], this);
		Integer startDate = null;
		Integer endDate = null;
		Integer todayStringInt = Integer.valueOf(new SimpleDateFormat("yyyyMMdd").format(new Date()));
		for (GCalendar gCalendar : gtfs.calendars) {
			if (gCalendar.start_date <= todayStringInt && gCalendar.end_date >= todayStringInt) {
				if (startDate == null || gCalendar.start_date < startDate) {
					startDate = gCalendar.start_date;
				}
				if (endDate == null || gCalendar.end_date > endDate) {
					endDate = gCalendar.end_date;
				}
			}
		}
		System.out.println("Generated on " + todayStringInt + " | Schedules from " + startDate + " to " + endDate);
		this.startWithFilters = new HashSet<String>();
		for (GCalendar gCalendar : gtfs.calendars) {
			if ((gCalendar.start_date >= startDate && gCalendar.start_date <= endDate) //
					|| (gCalendar.end_date >= startDate && gCalendar.end_date <= endDate)) {
				this.startWithFilters.add(gCalendar.service_id.substring(0, 6));
			}
		}
		for (GCalendarDate gCalendarDate : gtfs.calendarDates) {
			if (gCalendarDate.date >= startDate && gCalendarDate.date <= endDate) {
				this.startWithFilters.add(gCalendarDate.service_id.substring(0, 6));
			}
		}
		System.out.println("Filters: " + this.startWithFilters);
		gtfs = null;
		System.out.printf("Extracting useful service IDs... DONE\n");
	}

	@Override
	public boolean excludeRoute(GRoute gRoute) {
		if (ROUTE_TYPE_FILTER != null && !gRoute.route_type.equals(ROUTE_TYPE_FILTER)) {
			return true;
		}
		if (this.startWithFilters != null) {
			for (String startWithFilter : this.startWithFilters) {
				if (gRoute.route_id.startsWith(startWithFilter)) {
					return false; // keep
				}
			}
			return true; // exclude
		}
		return super.excludeRoute(gRoute);
	}

	@Override
	public boolean excludeTrip(GTrip gTrip) {
		if (this.startWithFilters != null) {
			for (String startWithFilter : this.startWithFilters) {
				if (gTrip.service_id.startsWith(startWithFilter)) {
					return false; // keep
				}
			}
			return true; // exclude
		}
		return super.excludeTrip(gTrip);
	}

	@Override
	public boolean excludeStop(GStop gStop) {
		if (this.startWithFilters != null) {
			for (String startWithFilter : this.startWithFilters) {
				if (gStop.stop_id.startsWith(startWithFilter)) {
					return false; // keep
				}
			}
			return true; // exclude
		}
		return super.excludeStop(gStop);
	}

	@Override
	public boolean excludeCalendarDate(GCalendarDate gCalendarDates) {
		if (this.startWithFilters != null) {
			for (String startWithFilter : this.startWithFilters) {
				if (gCalendarDates.service_id.startsWith(startWithFilter)) {
					return false; // keep
				}
			}
			return true; // exclude
		}
		return super.excludeCalendarDate(gCalendarDates);
	}

	@Override
	public boolean excludeCalendar(GCalendar gCalendar) {
		if (this.startWithFilters != null) {
			for (String startWithFilter : startWithFilters) {
				if (gCalendar.service_id.startsWith(startWithFilter)) {
					return false; // keep
				}
			}
			return true; // exclude
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
		result = MSpec.SAINT.matcher(result).replaceAll(MSpec.SAINT_REPLACEMENT);
		return MSpec.cleanLabel(result);
	}

	public static final String COLOR_BLUE = "0053A6";
	public static final String COLOR_WHITE = "FFFFFF";

	@Override
	public String getRouteColor(GRoute gRoute) {
		return COLOR_BLUE; // source file colors are nuts!
	}

	@Override
	public String getRouteTextColor(GRoute gRoute) {
		return COLOR_WHITE; // source file colors are nuts!
	}

	@Override
	public int getStopId(GStop gStop) {
		return Integer.valueOf(gStop.stop_code); // use stop code instead of stop ID
	}

	@Override
	public void setTripHeadsign(MRoute route, MTrip mTrip, GTrip gTrip) {
		final String directionIdString = gTrip.getRouteId().substring(gTrip.getRouteId().length() - 1);
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
		result = MSpec.CLEAN_SLASHES.matcher(result).replaceAll(MSpec.CLEAN_SLASHES_REPLACEMENT);
		result = Utils.replaceAll(result, START_WITH_FACES, MSpec.SPACE);
		result = Utils.replaceAll(result, SPACE_FACES, MSpec.SPACE);
		return super.cleanStopNameFR(result);
	}

}
