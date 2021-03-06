package com.dslplatform.json.generated.types.Timestamp;



import com.dslplatform.json.generated.types.StaticJson;
import com.dslplatform.json.generated.ocd.javaasserts.TimestampAsserts;

import java.io.IOException;

public class NullableTimestampDefaultValueTurtle {
	private static StaticJson.JsonSerialization jsonSerialization;

	@org.junit.BeforeClass
	public static void initializeJsonSerialization() throws IOException {
		jsonSerialization = StaticJson.getSerialization();
	}

	@org.junit.Test
	public void testDefaultValueEquality() throws IOException {
		final org.joda.time.DateTime defaultValue = null;
		final StaticJson.Bytes defaultValueJsonSerialized = jsonSerialization.serialize(defaultValue);
		final org.joda.time.DateTime defaultValueJsonDeserialized = jsonSerialization.deserialize(org.joda.time.DateTime.class, defaultValueJsonSerialized.content, defaultValueJsonSerialized.length);
		TimestampAsserts.assertNullableEquals(defaultValue, defaultValueJsonDeserialized);
	}

	@org.junit.Test
	public void testBorderValue1Equality() throws IOException {
		final org.joda.time.DateTime borderValue1 = org.joda.time.DateTime.now();
		final StaticJson.Bytes borderValue1JsonSerialized = jsonSerialization.serialize(borderValue1);
		final org.joda.time.DateTime borderValue1JsonDeserialized = jsonSerialization.deserialize(org.joda.time.DateTime.class, borderValue1JsonSerialized.content, borderValue1JsonSerialized.length);
		TimestampAsserts.assertNullableEquals(borderValue1, borderValue1JsonDeserialized);
	}

	@org.junit.Test
	public void testBorderValue2Equality() throws IOException {
		final org.joda.time.DateTime borderValue2 = new org.joda.time.DateTime(0);
		final StaticJson.Bytes borderValue2JsonSerialized = jsonSerialization.serialize(borderValue2);
		final org.joda.time.DateTime borderValue2JsonDeserialized = jsonSerialization.deserialize(org.joda.time.DateTime.class, borderValue2JsonSerialized.content, borderValue2JsonSerialized.length);
		TimestampAsserts.assertNullableEquals(borderValue2, borderValue2JsonDeserialized);
	}

	@org.junit.Test
	public void testBorderValue3Equality() throws IOException {
		final org.joda.time.DateTime borderValue3 = new org.joda.time.DateTime(1, 1, 1, 0, 0, org.joda.time.DateTimeZone.UTC);
		final StaticJson.Bytes borderValue3JsonSerialized = jsonSerialization.serialize(borderValue3);
		final org.joda.time.DateTime borderValue3JsonDeserialized = jsonSerialization.deserialize(org.joda.time.DateTime.class, borderValue3JsonSerialized.content, borderValue3JsonSerialized.length);
		TimestampAsserts.assertNullableEquals(borderValue3, borderValue3JsonDeserialized);
	}

	@org.junit.Test
	public void testBorderValue4Equality() throws IOException {
		final org.joda.time.DateTime borderValue4 = new org.joda.time.DateTime(Integer.MAX_VALUE * 1001L);
		final StaticJson.Bytes borderValue4JsonSerialized = jsonSerialization.serialize(borderValue4);
		final org.joda.time.DateTime borderValue4JsonDeserialized = jsonSerialization.deserialize(org.joda.time.DateTime.class, borderValue4JsonSerialized.content, borderValue4JsonSerialized.length);
		TimestampAsserts.assertNullableEquals(borderValue4, borderValue4JsonDeserialized);
	}
}
