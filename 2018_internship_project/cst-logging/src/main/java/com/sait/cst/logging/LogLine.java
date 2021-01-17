package com.sait.cst.logging;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class LogLine {
	private long timestamp;
	private String jsonString;
	private boolean jsonParsed;
	private JSONObject jsonObject;
	
	/**
	 * Creates an instance of LogLine class which is used to hold data captured from actual devices.
	 */
	public LogLine(long timestamp, String jsonString) {
		this.timestamp = timestamp;
		this.jsonString = jsonString;
		this.jsonObject = null;
		this.jsonParsed = false;
	}

	/**
	 * This method is used to serialize a given LogLine instance as a String so we can use it to
	 * write to a file.
	 */
	public static String serialize(LogLine logLine) {
		return logLine.timestamp + "," + logLine.jsonString;
	}
	
	/**
	 * This method is used to deserialize a given LogLine instance from a String value. This is almost
	 * always read from a file which is written to using serialize method above.
	 *
	 * It consists of two parts separated by a comma (,): timestamp and json.
	 */
	public static LogLine deserailize(String logLineString) {
		String[] lineSegments = logLineString.split(",", 2);
		
		if (lineSegments.length != 2) {
			return null;
		}

		long timestamp = Long.parseLong(lineSegments[0]);
		String jsonString = lineSegments[1];
		
		return new LogLine(timestamp, jsonString);
	}

	/**
	 * Returns whether or not timestamp provided is before LogLine's timestamp.
	 */
	public boolean isBefore(long otherTimestamp) {
		return timestamp < otherTimestamp;
	}
	
	/**
	 * Returns whether or not timestamp provided is after LogLine's timestamp.
	 */
	public boolean isAfter(long otherTimestamp) {
		return timestamp > otherTimestamp;
	}

	/**
	 * Returns whether or not JSON object held by LogLine is of expected type (e.g. fm).
	 */
	public boolean is(String expectedType) {
		parse();
		return jsonObject != null && jsonObject.has(expectedType);
	}

	/**
	 * This method returns fields for the given LogLine in a map where key is the field name and
	 * value is the field value. For example, for "fm" LogLine, map would have entries such as
	 * DFS State -> 3.
	 *
	 * Callers of this method use results to print results to the screen.
	 *
	 * Only a subset of LogLine types are supported: fm, cc, hc, sm. Other types will yield an
	 * empty map.
	 */
	public Map<String, String> getFields() {
		Map<String, String> fields = new LinkedHashMap<>();

		// all rows will include a timestamp for convenience.
		// timestamp will be formatted as ISO_INSTANT (truncated to seconds). milliseconds aren't useful for our case.
		fields.put("Timestamp (UTC)      ", DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(timestamp).truncatedTo(ChronoUnit.SECONDS)));

		if (is("fm")) {
			
			int rsInt = jsonObject.getJSONObject("fm").getInt("rs");
			fields.put(LogLineColumnNames.DFS_STATE, Integer.toString(rsInt));

			JSONArray ecArray = jsonObject.getJSONObject("fm").getJSONArray("ec[]");
			for (int i = 0; i < ecArray.length(); i++) {
				fields.put(LogLineColumnNames.DFS_ERROR, Integer.toString(ecArray.getInt(0)));
			}

		    JSONArray llMatrix = jsonObject.getJSONObject("fm").getJSONArray("ll[]");
		    for (int i = 0; i < llMatrix.length(); i++) {
		    	JSONArray llArray = llMatrix.getJSONArray(i);
		    	fields.put(LogLineColumnNames.DFS_LINE_LEVEL_1, Float.toString(llArray.getFloat(0)));
		    	fields.put(LogLineColumnNames.DFS_LINE_LEVEL_2, Float.toString(llArray.getFloat(1)));
		    }

		    JSONArray paaMatrix = jsonObject.getJSONObject("fm").getJSONArray("paa[]");
		    for (int i = 0; i < paaMatrix.length(); i++) {
		        JSONArray paaArray = paaMatrix.getJSONArray(i);
		        for (int j = 0; j < 4; j++) {
		        	if (i == 0) {
		        		if (paaMatrix.length() == 1) {
		        			String name = String.format("PA%d %s" , j + 1, LogLineColumnNames.PA_ALARM);
		        			fields.put(name, Integer.toString(paaArray.getInt(j)));
		        		} else {
		        			String name = String.format("CMBNR %s", LogLineColumnNames.PA_ALARM);
				        	fields.put(name, Integer.toString(paaArray.getInt(i)));
		        		}
		        	}
		        	else if (i == 1) {
		        		String name = String.format("MSTR PA%d %s", j + 1, LogLineColumnNames.PA_ALARM);
			        	fields.put(name, Integer.toString(paaArray.getInt(j)));
		        	}
		        	else {
		        		String name = String.format("SLV_%d PA%d %s", i - 1, j + 1, LogLineColumnNames.PA_ALARM);
		        		fields.put(name, Integer.toString(paaArray.getInt(j)));
		        	}
		        }
		    }

		    JSONArray saaMatrix = jsonObject.getJSONObject("fm").getJSONArray("saa[]");
		    for (int i = 0; i < saaMatrix.length(); i++) {
		        JSONArray saaArray = saaMatrix.getJSONArray(i);
		        for (int j = 0; j < 2; j++) {
		        	if (i == 0) {
		        		if (saaMatrix.length() == 1) {
		        			String name = String.format("PS%d %s", j + 1, LogLineColumnNames.PS_ALARM);
				        	fields.put(name, Integer.toString(saaArray.getInt(j)));
		        		}
		        		else {break;}
		        	}
		        	else if (i == 1) {
		        		String name = String.format("MSTR PS%d %s", j + 1, LogLineColumnNames.PS_ALARM);
			        	fields.put(name, Integer.toString(saaArray.getInt(j)));
		        	}
		        	else {
		        		String name = String.format("SLV_%d PS%d %s", i - 1, j + 1, LogLineColumnNames.PS_ALARM);
			        	fields.put(name, Integer.toString(saaArray.getInt(j)));
		        	}
		        }
		    }
		    
		} else if (is("cc")) {
			
		    int cccfInt = jsonObject.getJSONObject("cc").getInt("cf");
		    fields.put(LogLineColumnNames.DFS_CENTRE_FREQUENCY, Integer.toString(cccfInt));

		    int ccslInt = jsonObject.getJSONObject("cc").getInt("sl");
		    fields.put(LogLineColumnNames.DFS_SOURCE_LSB, Integer.toString(ccslInt));

		    int ccsuInt = jsonObject.getJSONObject("cc").getInt("su");
		    fields.put(LogLineColumnNames.DFS_SOURCE_USB, Integer.toString(ccsuInt));

		    int ccclInt = jsonObject.getJSONObject("cc").getInt("cl");
		    fields.put(LogLineColumnNames.DFS_CARRIER_LEVEL, Integer.toString(ccclInt));

		    float cclaFloat = jsonObject.getJSONObject("cc").getFloat("la");
		    fields.put(LogLineColumnNames.DFS_LEVEL_ADJUSTMENT, Float.toString(cclaFloat));

		    JSONArray ccfArray = jsonObject.getJSONObject("cc").getJSONArray("f[]");
		    for (int i = 0; i < ccfArray.length(); i++) {
		    	fields.put(LogLineColumnNames.DFS_TONE_1_FREQUENCY, Float.toString(ccfArray.getFloat(0)));
		    	fields.put(LogLineColumnNames.DFS_TONE_2_FREQUENCY, Float.toString(ccfArray.getFloat(1)));
		    }

		    JSONArray cctArray = jsonObject.getJSONObject("cc").getJSONArray("t[]");
		    for (int i = 0; i < cctArray.length(); i++) {
		    	fields.put(LogLineColumnNames.DFS_TONE_1_TIME, Float.toString(cctArray.getFloat(0)));
		    	fields.put(LogLineColumnNames.DFS_TONE_2_TIME, Float.toString(cctArray.getFloat(1)));
		    }

		    JSONArray cckmArray = jsonObject.getJSONObject("cc").getJSONArray("km[]");
		    for (int i = 0; i < cckmArray.length(); i++) {
		    	fields.put(LogLineColumnNames.DFS_KEY_MASK, Integer.toString(cckmArray.getInt(0)));
		    }

		    int ccplInt = jsonObject.getJSONObject("cc").getInt("pl");
		    fields.put(LogLineColumnNames.DFS_POWER_LEVEL, Integer.toString(ccplInt));

		    int ccvdInt = jsonObject.getJSONObject("cc").getInt("vd");
		    fields.put(LogLineColumnNames.DFS_VDAC, Integer.toString(ccvdInt));

		    int cczrInt = jsonObject.getJSONObject("cc").getInt("zr");
		    fields.put(LogLineColumnNames.DFS_AMP_ZRATIO, Integer.toString(cczrInt));

		    float cclmFloat = jsonObject.getJSONObject("cc").getFloat("lm");
		    fields.put(LogLineColumnNames.DFS_LEVEL_MAX, Float.toString(cclmFloat));
		    
		} else if (is("hc")) {
			
		    JSONArray u1fvArray = jsonObject.getJSONObject("hc").getJSONArray("u1fv[]");
		    for (int i = 0; i < u1fvArray.length(); i++) {
		        fields.put(LogLineColumnNames.DFS_U1_FIRMWARE_VERSION, u1fvArray.getString(0));
		    }

		    JSONArray u1snArray = jsonObject.getJSONObject("hc").getJSONArray("u1sn[]");
		    for (int i = 0; i < u1snArray.length(); i++) {
		        fields.put(LogLineColumnNames.DFS_SERIAL_NUMBER, Integer.toString(u1snArray.getInt(0)));
		    }

		    JSONArray u2pArray = jsonObject.getJSONObject("hc").getJSONArray("u2p[]");
		    for (int i = 0; i < u2pArray.length(); i++) {
		        fields.put(LogLineColumnNames.DFS_U2_COMM_ERRORS, Integer.toString(u2pArray.getInt(0)));
		    }

		    JSONArray u2fvArray = jsonObject.getJSONObject("hc").getJSONArray("u2fv[]");
		    for (int i = 0; i < u2fvArray.length(); i++) {
		        fields.put(LogLineColumnNames.DFS_U2_FIRMWARE_VERSION, u2fvArray.getString(0));
		    }

		    JSONArray u2pafvMatrix = jsonObject.getJSONObject("hc").getJSONArray("u2pafv[]");
		    for (int i = 0; i < u2pafvMatrix.length(); i++) {
		        JSONArray u2pafvArray = u2pafvMatrix.getJSONArray(i);
		        for (int j = 0; j < 4; j++) {
		        	if (i == 0) {
		        		if (u2pafvMatrix.length() == 1) {
		        			String name = String.format("PA%d %s", j + 1, LogLineColumnNames.PA_FIRMWARE_VERSION);
				        	fields.put(name, u2pafvArray.getString(j));
		        		} else {
		        			String name = String.format("CMBNR %s", LogLineColumnNames.PA_FIRMWARE_VERSION);
				        	fields.put(name, u2pafvArray.getString(i));
		        		}
		        	}
		        	else if (i == 1) {
		        		String name = String.format("MSTR PA%d %s", j + 1, LogLineColumnNames.PA_FIRMWARE_VERSION);
			        	fields.put(name, u2pafvArray.getString(j));
		        	}
		        	else {
		        		String name = String.format("SLV_%d PA%d %s", i - 1, j + 1, LogLineColumnNames.PA_FIRMWARE_VERSION);
			        	fields.put(name, u2pafvArray.getString(j));
		        	}	        	
		        }
		    }

		    JSONArray u2pasnMatrix = jsonObject.getJSONObject("hc").getJSONArray("u2pasn[]");
		    for (int i = 0; i < u2pasnMatrix.length(); i++) {
		        JSONArray u2pasnArray = u2pasnMatrix.getJSONArray(i);
		        for (int j = 0; j < 4; j++) {
		        	if (i == 0) {
		        		if (u2pasnMatrix.length() == 1) {
			        		String name = String.format("PA%d %s", j +1, LogLineColumnNames.PA_SERIAL_NUMBER);
				        	fields.put(name, Integer.toString(u2pasnArray.getInt(j)));
		        		} else {
			        		String name = String.format("CMBNR %s", LogLineColumnNames.PA_SERIAL_NUMBER);
				        	fields.put(name, Integer.toString(u2pasnArray.getInt(i)));
		        		}
		        	}
		        	else if (i == 1) {
		        		String name = String.format("MSTR PA%d %s", j + 1, LogLineColumnNames.PA_SERIAL_NUMBER);
			        	fields.put(name, Integer.toString(u2pasnArray.getInt(j)));
		        	}
		        	else {
		        		String name = String.format("SLV_%d PA%d %s", i - 1, j + 1, LogLineColumnNames.PA_SERIAL_NUMBER);
			        	fields.put(name, Integer.toString(u2pasnArray.getInt(j)));
		        	}
		        }
		    }

		    JSONArray u2pspMatrix = jsonObject.getJSONObject("hc").getJSONArray("u2psp[]");
		    for (int i = 0; i < u2pspMatrix.length(); i++) {
		        JSONArray u2pspArray = u2pspMatrix.getJSONArray(i);
		        for (int j = 0; j < 2; j++) {
		        	if (i == 0) {
		        		if (u2pspMatrix.length() == 1) {
		        			String name = String.format("PS%d %s", j + 1, LogLineColumnNames.PS_COMM_ERRORS);
				        	fields.put(name, Integer.toString(u2pspArray.getInt(j)));
		        		} else {break;}
		        	}
		        	else if (i == 1) {
		        		String name = String.format("MSTR PS%d %s", j + 1, LogLineColumnNames.PS_COMM_ERRORS);
			        	fields.put(name, Integer.toString(u2pspArray.getInt(j)));
		        	}
		        	else {
		        		String name = String.format("SLV_%d PS%d %s", i - 1, j + 1, LogLineColumnNames.PS_COMM_ERRORS);
			        	fields.put(name, Integer.toString(u2pspArray.getInt(j)));
		        	}
		        }
		    }

		    JSONArray u2pssnMatrix = jsonObject.getJSONObject("hc").getJSONArray("u2pssn[]");
		    for (int i = 0; i < u2pssnMatrix.length(); i++) {
		        JSONArray u2pssnArray = u2pssnMatrix.getJSONArray(i);
		        for (int j = 0; j < 2; j++) {
		        	if (i == 0) {
		        		if (u2pssnMatrix.length() == 1) {
		        			String name = String.format("PS%d %s", j + 1, LogLineColumnNames.PS_SERIAL_NUMBER);
				        	fields.put(name, u2pssnArray.getString(j));
		        		} else {break;}
		        	}
		        	else if (i == 1) {
		        		String name = String.format("MSTR PS%d %s", j + 1, LogLineColumnNames.PS_SERIAL_NUMBER);
			        	fields.put(name, u2pssnArray.getString(j));
		        	}
		        	else {
		        		String name = String.format("SLV_%d PS%d %s", i - 1, j + 1, LogLineColumnNames.PS_SERIAL_NUMBER);
			        	fields.put(name, u2pssnArray.getString(j));
		        	}
		        }
		    }
		    
		} else if (is("sm")) {
		    
			JSONArray isMatrix = jsonObject.getJSONObject("sm").getJSONArray("is[]");
		    for (int i = 0; i < isMatrix.length(); i++) {
		        JSONArray isArray = isMatrix.getJSONArray(i);
		        for (int j = 0; j < 2; j++) {
		        	if (i == 0) {
		        		if (isMatrix.length() == 1) {
		        			String name = String.format("PS%d %s", j + 1, LogLineColumnNames.PS_SUPPLY_CURRENT);
			        		fields.put(name, Integer.toString(isArray.getInt(j)));
		        		} else {break;}
		        	}
		        	else if (i == 1) {
		        		String name = String.format("MSTR PS%d %s", j + 1, LogLineColumnNames.PS_SUPPLY_CURRENT);
			        	fields.put(name, Integer.toString(isArray.getInt(j)));
		        	}
		        	else {
		        		String name = String.format("SLV_%d PS%d %s", i - 1, j + 1, LogLineColumnNames.PS_SUPPLY_CURRENT);
			        	fields.put(name, Integer.toString(isArray.getInt(j)));
		        	}
		        }
		    }

		    JSONArray vsMatrix = jsonObject.getJSONObject("sm").getJSONArray("vs[]");
		    for (int i = 0; i < vsMatrix.length(); i++) {
		        JSONArray vsArray = vsMatrix.getJSONArray(i);
		        for (int j = 0; j < 2; j++) {
		        	if (i == 0) {
		        		if (vsMatrix.length() == 1) {
		        			String name = String.format("PS%d %s", j + 1, LogLineColumnNames.PS_DC_VOLTAGE);
				        	fields.put(name, Integer.toString(vsArray.getInt(j)));
		        		} else {break;}
		        	}
		        	else if (i == 1) {
		        		String name = String.format("MSTR PS%d %s", j + 1, LogLineColumnNames.PS_DC_VOLTAGE);
			        	fields.put(name, Integer.toString(vsArray.getInt(j)));
		        	}
		        	else {
		        		String name = String.format("SLV_%d PS%d %s", i - 1, j + 1, LogLineColumnNames.PS_DC_VOLTAGE);
			        	fields.put(name, Integer.toString(vsArray.getInt(j)));
		        	}
		        }
		    }

		    JSONArray tsMatrix = jsonObject.getJSONObject("sm").getJSONArray("ts[]");
		    for (int i = 0; i < tsMatrix.length(); i++) {
		        JSONArray tsArray = tsMatrix.getJSONArray(i);
		        for (int j = 0; j < 2; j++) {
		        	if (i == 0) {
		        		if (tsMatrix.length() == 1) {
		        			String name = String.format("PS%d %s", j + 1, LogLineColumnNames.PS_HEATSINK_TEMPERATURE);
				        	fields.put(name, Integer.toString(tsArray.getInt(j)));
		        		} else {break;}
		        	}
		        	else if (i == 1) {
		        		String name = String.format("MSTR PS%d %s", j + 1, LogLineColumnNames.PS_HEATSINK_TEMPERATURE);
			        	fields.put(name, Integer.toString(tsArray.getInt(j)));
		        	}
		        	else {
		        		String name = String.format("SLV_%d PS%d %s", i - 1, j + 1, LogLineColumnNames.PS_HEATSINK_TEMPERATURE);
			        	fields.put(name, Integer.toString(tsArray.getInt(j)));
		        	}
		        }
		    }

		    JSONArray iaMatrix = jsonObject.getJSONObject("sm").getJSONArray("ia[]");
		    for (int i = 0; i < iaMatrix.length(); i++) {
		        JSONArray iaArray = iaMatrix.getJSONArray(i);
		        for (int j = 0; j < 4; j++) {
		        	if (i == 0) {
		        		if (iaMatrix.length() == 1) {
		        			String name = String.format("PA%d %s", j + 1, LogLineColumnNames.PA_SUPPLY_CURRENT);
				        	fields.put(name, Integer.toString(iaArray.getInt(j)));
		        		} else {
		        			String name = String.format("CMBNR %s", LogLineColumnNames.PA_SUPPLY_CURRENT);
				        	fields.put(name, Integer.toString(iaArray.getInt(i)));	
		        		}
		        	}
		        	else if (i == 1) {
		        		String name = String.format("MSTR PA%d %s", j + 1, LogLineColumnNames.PA_SUPPLY_CURRENT);
			        	fields.put(name, Integer.toString(iaArray.getInt(j)));
		        	}
		        	else {
		        		String name = String.format("SLV_%d PA%d %s", i - 1, j + 1, LogLineColumnNames.PA_SUPPLY_CURRENT);
			        	fields.put(name, Integer.toString(iaArray.getInt(j)));
		        	}
		        }
		    }

		    JSONArray taMatrix = jsonObject.getJSONObject("sm").getJSONArray("ta[]");
		    for (int i = 0; i < taMatrix.length(); i++) {
		        JSONArray taArray = taMatrix.getJSONArray(i);
		        for (int j = 0; j < 4; j++) {
		        	if (i == 0) {
		        		if (taMatrix.length() == 1) {
		        			String name = String.format("PA%d %s", j + 1, LogLineColumnNames.PA_HEATSINK_TEMPERATURE);
				        	fields.put(name, Integer.toString(taArray.getInt(j)));
		        		} else {
		        			for (int k = 0; k < 2; k++) {
				        		String name = String.format("CMBNR %s %d", LogLineColumnNames.PA_HEATSINK_TEMPERATURE, k + 1);
					        	fields.put(name, Integer.toString(taArray.getInt(k)));
			        		}
		        		}
		        	}
		        	else if (i == 1) {
		        		String name = String.format("MSTR PA%d %s", j + 1, LogLineColumnNames.PA_HEATSINK_TEMPERATURE);
			        	fields.put(name, Integer.toString(taArray.getInt(j)));
		        	}
		        	else {
		        		String name = String.format("SLV_%d PA%d %s", i - 1, j + 1, LogLineColumnNames.PA_HEATSINK_TEMPERATURE);
			        	fields.put(name, Integer.toString(taArray.getInt(j)));
		        	}
		        }
		    }

		    JSONArray faMatrix = jsonObject.getJSONObject("sm").getJSONArray("fa[]");
		    for (int i = 0; i < faMatrix.length(); i++) {
		        JSONArray faArray = faMatrix.getJSONArray(i);
		        for (int j = 0; j < 4; j++) {
		        	if (i == 0) {
		        		if (faMatrix.length() == 1) {
		        			String name = String.format("PA%d %s", j + 1, LogLineColumnNames.PA_FAN_SPEED);
				        	fields.put(name, Integer.toString(faArray.getInt(j)));
		        		} else {break;}
		        	}
		        	else if (i == 1) {
		        		String name = String.format("MSTR PA%d %s", j + 1, LogLineColumnNames.PA_FAN_SPEED);
			        	fields.put(name, Integer.toString(faArray.getInt(j)));
		        	}
		        	else {
		        		String name = String.format("SLV_%d PA%d %s", i - 1, j + 1, LogLineColumnNames.PA_FAN_SPEED);
			        	fields.put(name, Integer.toString(faArray.getInt(j)));
		        	}
		        }
		    }

		    JSONArray paMatrix = jsonObject.getJSONObject("sm").getJSONArray("pa[]");
		    for (int i = 0; i < paMatrix.length(); i++) {
		        JSONArray paArray = paMatrix.getJSONArray(i);
		        for (int j = 0; j < 4; j++) {
		        	if (i == 0) {
		        		if (paMatrix.length() == 1) {
		        			String name = String.format("PA%d %s", j + 1, LogLineColumnNames.PA_RF_LEVEL_OUT);
				        	fields.put(name, Integer.toString(paArray.getInt(j)));
		        		} else {break;}
		        	}
		        	else if (i == 1) {
		        		String name = String.format("MSTR PA%d %s", j + 1, LogLineColumnNames.PA_RF_LEVEL_OUT);
			        	fields.put(name, Integer.toString(paArray.getInt(j)));
		        	}
		        	else {
		        		String name = String.format("SLV_%d PA%d %s", i - 1, j + 1, LogLineColumnNames.PA_RF_LEVEL_OUT);
			        	fields.put(name, Integer.toString(paArray.getInt(j)));
		        	}
		        }
		    }

		    JSONArray vcMatrix = jsonObject.getJSONObject("sm").getJSONArray("vc[]");
		    for (int i = 0; i < vcMatrix.length(); i++) {
		        JSONArray vcArray = vcMatrix.getJSONArray(i);
		        for (int j = 0; j < 4; j++) {
		        	if (i == 0) {
		        		if (vcMatrix.length() == 1) {
		        			String name = String.format("PA%d %s", j + 1, LogLineColumnNames.PA_RF_LEVEL_COMB);
				        	fields.put(name, Integer.toString(vcArray.getInt(j)));
		        		} else {break;}
		        	}
		        	else if (i == 1) {
		        		String name = String.format("MSTR PA%d %s", j + 1, LogLineColumnNames.PA_RF_LEVEL_COMB);
			        	fields.put(name, Integer.toString(vcArray.getInt(j)));
		        	}
		        	else {
		        		String name = String.format("SLV_%d PA%d %s", i - 1, j + 1, LogLineColumnNames.PA_RF_LEVEL_COMB);
			        	fields.put(name, Integer.toString(vcArray.getInt(j)));
		        	}
		        }
		    }

		    JSONArray omMatrix = jsonObject.getJSONObject("sm").getJSONArray("om[]");
		    for (int i = 0; i < omMatrix.length(); i++) {
		        JSONArray omArray = omMatrix.getJSONArray(i);
		        for (int j = 0; j < 4; j++) {
		        	if (i == 0) {
		        		if (omMatrix.length() == 1) {
		        			String name = String.format("PA%d %s", j + 1, LogLineColumnNames.PA_MAIN_ON_TIME);
				        	fields.put(name, Integer.toString(omArray.getInt(j)));	
		        		} else {
		        			String name = String.format("CMBNR %s", LogLineColumnNames.PA_MAIN_ON_TIME);
				        	fields.put(name, Integer.toString(omArray.getInt(i)));
		        		}
		        	}
		        	else if (i == 1) {
		        		String name = String.format("MSTR PA%d %s", j + 1, LogLineColumnNames.PA_MAIN_ON_TIME);
			        	fields.put(name, Integer.toString(omArray.getInt(j)));
		        	}
		        	else {
		        		String name = String.format("SLV_%d PA%d %s", i - 1, j + 1, LogLineColumnNames.PA_MAIN_ON_TIME);
			        	fields.put(name, Integer.toString(omArray.getInt(j)));
		        	}
		        }
		    }

		    JSONArray obMatrix = jsonObject.getJSONObject("sm").getJSONArray("ob[]");
		    for (int i = 0; i < obMatrix.length(); i++) {
		        JSONArray obArray = obMatrix.getJSONArray(i);
		        for (int j = 0; j < 4; j++) {
		        	if (i == 0) {
		        		if (obMatrix.length() == 1) {
		        			String name = String.format("PA%d %s", j + 1, LogLineColumnNames.PA_BIAS_ON_TIME);
				        	fields.put(name, Integer.toString(obArray.getInt(j)));
		        		} else {
		        			String name = String.format("CMBNR %s", LogLineColumnNames.PA_BIAS_ON_TIME);
				        	fields.put(name, Integer.toString(obArray.getInt(i)));
		        		}
		        	}
		        	else if (i == 1) {
		        		String name = String.format("MSTR PA%d %s", j + 1, LogLineColumnNames.PA_BIAS_ON_TIME);
			        	fields.put(name, Integer.toString(obArray.getInt(j)));
		        	}
		        	else {
		        		String name = String.format("SLV_%d PA%d %s", i - 1, j + 1, LogLineColumnNames.PA_BIAS_ON_TIME);
			        	fields.put(name, Integer.toString(obArray.getInt(j)));
		        	}
		        	
		        }
		    }

		    JSONArray ofMatrix = jsonObject.getJSONObject("sm").getJSONArray("of[]");
		    for (int i = 0; i < ofMatrix.length(); i++) {
		        JSONArray ofArray = ofMatrix.getJSONArray(i);
		        for (int j = 0; j < 4; j++) {
		        	if (i == 0) {
		        		if (ofMatrix.length() == 1) {
		        			String name = String.format("PA%d %s", j + 1, LogLineColumnNames.PA_FAN_ON_TIME);
				        	fields.put(name, Integer.toString(ofArray.getInt(j)));
		        		} else {
		        			String name = String.format("CMBNR %s", LogLineColumnNames.PA_FAN_ON_TIME);
				        	fields.put(name, Integer.toString(ofArray.getInt(i)));
		        		}	
		        	}
		        	else if (i == 1) {
		        		String name = String.format("MSTR PA%d %s", j + 1, LogLineColumnNames.PA_FAN_ON_TIME);
			        	fields.put(name, Integer.toString(ofArray.getInt(j)));
		        	}
		        	else {
		        		String name = String.format("SLV_%d PA%d %s", i - 1, j + 1, LogLineColumnNames.PA_FAN_ON_TIME);
			        	fields.put(name, Integer.toString(ofArray.getInt(j)));
		        	}
		        }
		    }

		    JSONArray biArray = jsonObject.getJSONObject("sm").getJSONArray("bi[]");
		    for (int i = 0; i < biArray.length(); i++) {
		    	if (i == 0) {
		    		if (biArray.length() == 1) {
		    			String name = String.format("%s", LogLineColumnNames.DFS_BIAS);
			    		fields.put(name, Integer.toString(biArray.getInt(i)));
		    		} else {break;}
		    	}
		    	else if (i == 1) {
		    		String name = String.format("MSTR %s", LogLineColumnNames.DFS_BIAS);
		    		fields.put(name, Integer.toString(biArray.getInt(i)));
		    	}
		    	else {
		    		String name = String.format("SLV_%d %s", i - 1, LogLineColumnNames.DFS_BIAS);
		    		fields.put(name, Integer.toString(biArray.getInt(i)));
		    	}
		    }
		    JSONArray meArray = jsonObject.getJSONObject("sm").getJSONArray("me[]");
		    for (int i = 0; i < meArray.length(); i++) {
		    	if (i == 0) {
		    		if (meArray.length() == 1) {
		    			String name = String.format("%s", LogLineColumnNames.PA_PS_ENABLE);
			        	fields.put(name, Integer.toString(meArray.getInt(i)));
		    		} else {break;}
		    	}
		    	else if (i == 1) {
		    		String name = String.format("MSTR %s", LogLineColumnNames.PA_PS_ENABLE);
		        	fields.put(name, Integer.toString(meArray.getInt(i)));
		    	}
		    	else {
		    		String name = String.format("SLV_%d %s", i - 1, LogLineColumnNames.PA_PS_ENABLE);
		        	fields.put(name, Integer.toString(meArray.getInt(i)));
		    	}
		    }
		   
		} else {
			LoggingUtils.WARN("Unexpected provided; don't know how to format this line: %s", jsonString);
		}

		return fields;
	}

	@Override
	public String toString() {
		return String.format("LogLine(timestamp=%d, jsonString=%s)", timestamp, jsonString);
	}

	/**
	 * This method is used to lazily parse the jsonString as a JSONObject for this LogLine. Since parsing
	 * JSONObject is a costly operation, we only do it when needed; it's not immediately done in the
	 * constructor.
	 */
	private void parse() {
		if (!jsonParsed) {
			try {
				jsonObject = new JSONObject(jsonString);
				jsonParsed = true;
			} catch (JSONException exception) {
				LoggingUtils.ERROR("Failed to parse string as JSON: %s", exception.getMessage());
			}
		}
	}
}
