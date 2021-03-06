package org.opensky.model;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Custom JSON deserializer for OpenSkyStates retrieved from the API.
 *
 * XXX Because actual state vectors arrive as array we need a custom deserializer like this.
 * If anyone comes up with something better, feel free to create a pull request!
 *
 * @author Markus Fuchs, fuchs@opensky-network.org
 */
public class OpenSkyStatesDeserializer extends StdDeserializer<OpenSkyStates> {
	public OpenSkyStatesDeserializer() {
		super(OpenSkyStates.class);
	}

	public Collection<StateVector> deserStates(JsonParser jp, DeserializationContext dc) throws IOException {
		ArrayList<StateVector> result = new ArrayList<StateVector>();

		for (JsonToken next = jp.nextToken(); next != null && next != JsonToken.END_ARRAY; next = jp.nextToken()) {
			if (next == JsonToken.START_ARRAY) {
				continue;
			}
			if (next == JsonToken.END_OBJECT) {
				break;
			}
			String icao24 = jp.getText();
			if ("null".equals(icao24)) {
				throw new JsonParseException("Got 'null' icao24", jp.getCurrentLocation());
			}

			StateVector sv = new StateVector(icao24);
			sv.setCallsign(jp.nextTextValue());
			sv.setOriginCountry(jp.nextTextValue());
			sv.setLastPositionUpdate((jp.nextToken() != null && jp.getCurrentToken() != JsonToken.VALUE_NULL ? jp.getDoubleValue() : null));
			sv.setLastVelocityUpdate((jp.nextToken() != null && jp.getCurrentToken() != JsonToken.VALUE_NULL ? jp.getDoubleValue() : null));
			sv.setLongitude((jp.nextToken() != null && jp.getCurrentToken() != JsonToken.VALUE_NULL ? jp.getDoubleValue() : null));
			sv.setLatitude((jp.nextToken() != null && jp.getCurrentToken() != JsonToken.VALUE_NULL ? jp.getDoubleValue() : null));
			sv.setAltitude((jp.nextToken() != null && jp.getCurrentToken() != JsonToken.VALUE_NULL ? jp.getDoubleValue() : null));
			sv.setOnGround(jp.nextBooleanValue());
			sv.setVelocity((jp.nextToken() != null && jp.getCurrentToken() != JsonToken.VALUE_NULL ? jp.getDoubleValue() : null));
			sv.setHeading((jp.nextToken() != null && jp.getCurrentToken() != JsonToken.VALUE_NULL ? jp.getDoubleValue() : null));
			sv.setVerticalRate((jp.nextToken() != null && jp.getCurrentToken() != JsonToken.VALUE_NULL ? jp.getDoubleValue() : null));

			// sensor serials if present
			next = jp.nextToken();
			if (next == JsonToken.START_ARRAY) {
				for (next = jp.nextToken(); next != null && next != JsonToken.END_ARRAY; next = jp.nextToken()) {
					sv.addSerial(jp.getIntValue());
				}
			} else {
				// serials not present (null) or there are additional fields (upward compatibility)
				// consume until end of this state vector array
				for (next = jp.nextToken(); next != null && next != JsonToken.END_ARRAY; next = jp.nextToken()) {
					// ignore
				}
			}
			// consume "END_ARRAY"
			jp.nextToken();

			result.add(sv);
		}

		return result;
	}

	@Override
	public OpenSkyStates deserialize(JsonParser jp, DeserializationContext dc) throws IOException, JsonProcessingException {
		if (jp.getCurrentToken() != null && jp.getCurrentToken() != JsonToken.START_OBJECT) {
			throw dc.mappingException(OpenSkyStates.class);
		}
		try {
			OpenSkyStates res = new OpenSkyStates();
			for (jp.nextToken(); jp.getCurrentToken() != null && jp.getCurrentToken() != JsonToken.END_OBJECT; jp.nextToken()) {
				if (jp.getCurrentToken() == JsonToken.FIELD_NAME) {
					if ("time".equalsIgnoreCase(jp.getCurrentName())) {
						int t = jp.nextIntValue(0);
						res.setTime(t);
					} else if ("states".equalsIgnoreCase(jp.getCurrentName())) {
						jp.nextToken();
						res.setStates(deserStates(jp, dc));
					} else {
						// ignore other fields, but consume value
						jp.nextToken();
					}
				} // ignore others
			}
			return res;
		} catch (JsonParseException jpe) {
			throw dc.mappingException(OpenSkyStates.class);
		}
	}
}
