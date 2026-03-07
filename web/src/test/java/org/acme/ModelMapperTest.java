package org.acme;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;

public class ModelMapperTest {

    private static String jsonContent;

    @BeforeAll
    public static void setUp() throws IOException {
        jsonContent = new String(ModelMapperTest.class.getResourceAsStream("request2.json").readAllBytes());
    }

    @Test
    public void testJsonToModelConversion() throws IOException {
        // Load the JSON from request2.json
        JsonObject requestJson = new JsonObject(jsonContent);

        // Verify properties
        JsonObject dataJson = requestJson.getJsonObject("data");
        assertNotNull(dataJson, "Data should exist in model");

        assertEquals("Network 0", dataJson.getString("shared_name"), "Shared name should be 'Network 0'");
        assertEquals("", dataJson.getString("networkMetadata"));
        assertEquals(52, dataJson.getInteger("SUID"), "SUID should be 52");
        assertEquals(1, dataJson.getInteger("seconds_per_point"), "Seconds per point should be 1");
        assertEquals(true, dataJson.getBoolean("selected"), "Selected should be true");
        assertEquals(100, dataJson.getInteger("levels"), "Levels should be 100");
    }


    @Test
    void testRequest2JsonNodesAndEdgesDataStructure() throws Exception {
        // Load the JSON file
        JSONObject jsonObject = new JSONObject(jsonContent);

        // Validate root structure
        assertNotNull(jsonObject.getJSONObject("elements"), "Elements object should exist");

        JSONObject elements = jsonObject.getJSONObject("elements");
        JSONArray nodes = elements.getJSONArray("nodes");
        JSONArray edges = elements.getJSONArray("edges");

        // Test nodes data
        assertEquals(3, nodes.length(), "Should have 3 nodes");

        for (int i = 0; i < nodes.length(); i++) {
            JSONObject node = nodes.getJSONObject(i);
            JSONObject nodeData = node.getJSONObject("data");

            // Validate required node fields
            assertTrue(nodeData.has("id"), "Node " + i + " should have id");
            assertTrue(nodeData.has("canonicalName"), "Node " + i + " should have canonicalName");
            assertTrue(nodeData.has("enabled"), "Node " + i + " should have enabled");
            assertTrue(nodeData.has("levels"), "Node " + i + " should have levels");
            assertTrue(nodeData.has("initialConcentration"), "Node " + i + " should have initialConcentration");

            String nodeId = nodeData.getString("id");
            boolean enabled = nodeData.getBoolean("enabled");
            int levels = nodeData.getInt("levels");

            assertNotNull(nodeId, "Node id should not be null");
            assertTrue(levels > 0, "Node " + nodeId + " levels should be positive");

            // Validate optional fields
            if (nodeData.has("moleculeType")) {
                assertNotNull(nodeData.getString("moleculeType"));
            }
            if (nodeData.has("Position_X") && nodeData.has("Position_Y")) {
                double posX = nodeData.getDouble("Position_X");
                double posY = nodeData.getDouble("Position_Y");
                assertFalse(Double.isNaN(posX), "Position_X should be a valid number");
                assertFalse(Double.isNaN(posY), "Position_Y should be a valid number");
            }
        }

        // Test edges data
        assertEquals(2, edges.length(), "Should have 2 edges");

        for (int i = 0; i < edges.length(); i++) {
            JSONObject edge = edges.getJSONObject(i);
            JSONObject edgeData = edge.getJSONObject("data");

            // Validate required edge fields
            assertTrue(edgeData.has("source"), "Edge " + i + " should have source");
            assertTrue(edgeData.has("target"), "Edge " + i + " should have target");
            assertTrue(edgeData.has("enabled"), "Edge " + i + " should have enabled");
            assertTrue(edgeData.has("increment"), "Edge " + i + " should have increment");
            assertTrue(edgeData.has("scenario"), "Edge " + i + " should have scenario");

            String source = edgeData.getString("source");
            String target = edgeData.getString("target");
            int increment = edgeData.getInt("increment");
            int scenario = edgeData.getInt("scenario");

            assertNotNull(source, "Edge source should not be null");
            assertNotNull(target, "Edge target should not be null");
            assertNotEquals(source, target, "Edge source and target should be different");
            assertTrue(increment == 1 || increment == -1, "Increment should be 1 or -1, got " + increment);
            assertTrue(scenario >= 0 && scenario <= 2, "Scenario should be 0, 1, or 2, got " + scenario);

            // Validate scenario-specific fields
            if (scenario == 2) {
                assertTrue(edgeData.has("_REACTANT_E1"), "Scenario 2 edge should have _REACTANT_E1");
                assertTrue(edgeData.has("_REACTANT_E2"), "Scenario 2 edge should have _REACTANT_E2");
                assertTrue(edgeData.has("_REACTANT_ACT_E1"), "Scenario 2 edge should have _REACTANT_ACT_E1");
                assertTrue(edgeData.has("_REACTANT_ACT_E2"), "Scenario 2 edge should have _REACTANT_ACT_E2");
            }

            // Validate source and target reference valid nodes
            boolean sourceExists = false;
            boolean targetExists = false;
            for (int j = 0; j < nodes.length(); j++) {
                String nodeId = nodes.getJSONObject(j).getJSONObject("data").getString("id");
                if (nodeId.equals(source)) sourceExists = true;
                if (nodeId.equals(target)) targetExists = true;
            }
            assertTrue(sourceExists, "Edge source " + source + " should reference a valid node");
            assertTrue(targetExists, "Edge target " + target + " should reference a valid node");
        }
    }
}
