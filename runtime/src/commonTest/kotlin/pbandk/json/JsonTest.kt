package pbandk.json

import kotlinx.serialization.json.*
import pbandk.ByteArr
import pbandk.protoMarshal
import pbandk.testpb.Bar
import pbandk.testpb.TestAllTypesProto3
import pbandk.wkt.Timestamp
import kotlin.test.*
import kotlin.test.assertEquals

class JsonTest {
    @Test
    fun testMessageField_null() {
        val json = "{ \"single_foo\": null }"
        val bar = Bar.jsonUnmarshal(json)
        assertNull(bar.singleFoo)

        val barBytes = bar.protoMarshal()
        assertEquals(emptyList(), barBytes.asList(), "binary serialization should be empty for null field")
    }

    @Test
    fun testBytesField_base64() {
        val testAllTypesProto3 = TestAllTypesProto3(optionalBytes = ByteArr(byteArrayOf(1, 2)))
        val expectedJson = buildJsonObject { put("optionalBytes", "AQI=") }

        val actualJson = Json.parseToJsonElement(testAllTypesProto3.jsonMarshal())
        assertEquals(expectedJson, actualJson)

        val actualProto = TestAllTypesProto3.jsonUnmarshal(expectedJson.toString())
        assertEquals(testAllTypesProto3, actualProto)
    }

    @Test
    fun testTimestampField() {
        val json = buildJsonObject { put("optionalTimestamp", "0001-01-01T00:00:00Z") }.toString()
        val expectedTimestamp = Timestamp(seconds = -62135596800, nanos = 0)

        val testAllTypesProto3 = TestAllTypesProto3.jsonUnmarshal(json)
        assertEquals(testAllTypesProto3.optionalTimestamp, expectedTimestamp)
    }

    @Test
    fun testTimestampField_withNanos() {
        val json = buildJsonObject { put("optionalTimestamp", "1993-02-10T00:00:00.000Z") }.toString()
        val expectedTimestamp = Timestamp(seconds = 729302400, nanos = 0)

        val testAllTypesProto3 = TestAllTypesProto3.jsonUnmarshal(json)
        assertEquals(testAllTypesProto3.optionalTimestamp, expectedTimestamp)
    }

    @Test
    fun testOutputDefaultValues_false() {
        val testAllTypesProto3 = TestAllTypesProto3(optionalString = "")

        val actualJson = Json.parseToJsonElement(testAllTypesProto3.jsonMarshal())
        assertFalse("optionalString" in actualJson.jsonObject)
    }

    @Test
    fun testOutputDefaultValues_true() {
        val testAllTypesProto3 = TestAllTypesProto3(optionalString = "")

        val actualJson = Json.parseToJsonElement(
            testAllTypesProto3.jsonMarshal(JsonConfig(outputDefaultValues = true))
        )
        assertTrue("optionalString" in actualJson.jsonObject)
    }
}