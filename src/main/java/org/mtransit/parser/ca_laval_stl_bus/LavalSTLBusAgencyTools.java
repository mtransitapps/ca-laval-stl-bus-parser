package org.mtransit.parser.ca_laval_stl_bus;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mtransit.commons.CleanUtils;
import org.mtransit.commons.RegexUtils;
import org.mtransit.commons.StringUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MTrip;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

// https://stlaval.ca/about-us/public-information/open-data
// https://stlaval.ca/a-propos/diffusion/donnees-ouvertes
public class LavalSTLBusAgencyTools extends DefaultAgencyTools {

	public static void main(@NotNull String[] args) {
		new LavalSTLBusAgencyTools().start(args);
	}

	@Nullable
	@Override
	public List<Locale> getSupportedLanguages() {
		return LANG_FR;
	}

	@Override
	public boolean defaultExcludeEnabled() {
		return true;
	}

	@NotNull
	@Override
	public String getAgencyName() {
		return "STL";
	}

	@NotNull
	@Override
	public Integer getAgencyRouteType() {
		return MAgency.ROUTE_TYPE_BUS;
	}

	@Override
	public boolean defaultRouteIdEnabled() {
		return true;
	}

	@Override
	public boolean useRouteShortNameForRouteId() {
		return true;
	}

	@Override
	public boolean defaultRouteLongNameEnabled() {
		return true;
	}

	@Override
	public @Nullable String getRouteIdCleanupRegex() {
		return "^[A-Z]+\\d{2}|[NSEO]$"; // route ID is like MMMMyyRSNd (ex: "JANV2412E") for 12 east
	}

	@NotNull
	@Override
	public String cleanRouteLongName(@NotNull String routeLongName) {
		routeLongName = CleanUtils.keepToFR(routeLongName);
		routeLongName = CleanUtils.SAINT.matcher(routeLongName).replaceAll(CleanUtils.SAINT_REPLACEMENT);
		routeLongName = CleanUtils.cleanBounds(Locale.FRENCH, routeLongName);
		routeLongName = CleanUtils.cleanStreetTypesFRCA(routeLongName);
		return CleanUtils.cleanLabel(routeLongName);
	}

	@Override
	public boolean defaultAgencyColorEnabled() {
		return true;
	}

	@Override
	public int getStopId(@NotNull GStop gStop) {
		return Integer.parseInt(gStop.getStopCode()); // use stop code instead of stop ID
	}

	@NotNull
	@Override
	public String provideMissingTripHeadSign(@NotNull GTrip gTrip) {
		//noinspection deprecation
		final String routeId = gTrip.getRouteId();
		return routeId.substring(routeId.length() - 1); // last character = E/O/N/S (not W)
	}

	@Override
	public boolean directionSplitterEnabled(long routeId) {
		return true; // BECAUSE direction_id NOT provided
	}

	@Override
	public boolean directionFinderEnabled() {
		return true; // actually not working BECAUSE direction_id NOT provided & 2 routes for 1 route w/ 2 directions
	}

	@NotNull
	@Override
	public List<Integer> getDirectionTypes() {
		return Collections.singletonList(
				MTrip.HEADSIGN_TYPE_DIRECTION // used by real-time API
		);
	}

	@NotNull
	@Override
	public String cleanTripHeadsign(@NotNull String tripHeadsign) {
		tripHeadsign = CleanUtils.keepToFR(tripHeadsign);
		tripHeadsign = CleanUtils.cleanStreetTypesFRCA(tripHeadsign);
		tripHeadsign = CleanUtils.CLEAN_ET.matcher(tripHeadsign).replaceAll(CleanUtils.CLEAN_ET_REPLACEMENT);
		return CleanUtils.cleanLabelFR(tripHeadsign);
	}

	private static final Pattern REMOVE_STOP_CODE_STOP_NAME = Pattern.compile("\\[[0-9]{5}]");

	private static final Pattern START_WITH_FACE_A = Pattern.compile("^(face à )", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.CANON_EQ);
	private static final Pattern START_WITH_FACE_AU = Pattern.compile("^(face au )", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
	private static final Pattern START_WITH_FACE = Pattern.compile("^(face )", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

	private static final Pattern SPACE_FACE_A = Pattern.compile("( face à )", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.CANON_EQ);
	private static final Pattern SPACE_WITH_FACE_AU = Pattern.compile("( face au )", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
	private static final Pattern SPACE_WITH_FACE = Pattern.compile("( face )", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

	private static final Pattern[] START_WITH_FACES = new Pattern[]{START_WITH_FACE_A, START_WITH_FACE_AU, START_WITH_FACE};

	private static final Pattern[] SPACE_FACES = new Pattern[]{SPACE_FACE_A, SPACE_WITH_FACE_AU, SPACE_WITH_FACE};

	@NotNull
	@Override
	public String cleanStopName(@NotNull String gStopName) {
		gStopName = REMOVE_STOP_CODE_STOP_NAME.matcher(gStopName).replaceAll(StringUtils.EMPTY);
		gStopName = CleanUtils.cleanSlashes(gStopName);
		gStopName = RegexUtils.replaceAllNN(gStopName, START_WITH_FACES, CleanUtils.SPACE);
		gStopName = RegexUtils.replaceAllNN(gStopName, SPACE_FACES, CleanUtils.SPACE);
		gStopName = CleanUtils.cleanStreetTypesFRCA(gStopName);
		return CleanUtils.cleanLabelFR(gStopName);
	}
}
