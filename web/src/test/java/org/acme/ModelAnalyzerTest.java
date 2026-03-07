package org.acme;

import animo.core.model.Model;
import animo.exceptions.AnimoException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.acme.domain.CytoscapeModel;
import org.acme.domain.SimulationJob;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

class ModelAnalyzerTest {

    @Test
    void getModelFromJson() throws JSONException, AnimoException, IOException, IllegalAccessException {
        InputStream is = ModelAnalyzerTest.class.getResourceAsStream("request2.json");
        CytoscapeModel job = new ObjectMapper().readValue(is, CytoscapeModel.class);
        JSONObject modelJson = new JSONObject(job);
        Model modelFromJson = ModelMapper.getModelFromJson(modelJson, 1);
        Model modelFromJson2 = ModelMapper.getModelFromJson(job, 1);
//        assertEquals(modelFromJson2.getProperties(), modelFromJson.getProperties());

        assertEquals(modelFromJson2.getProperties().get("maxTime").as(Integer.class),
                modelFromJson.getProperties().get("maxTime").as(Integer.class));

        assertEquals(modelFromJson2.getProperties().get("minTime").as(Integer.class),
                modelFromJson.getProperties().get("minTime").as(Integer.class));

        assertEquals(modelFromJson2.getProperties().get("time scale factor").as(Double.class),
                modelFromJson.getProperties().get("time scale factor").as(Double.class));

        assertEquals(modelFromJson2.getProperties().get("seconds per point").as(Double.class),
                modelFromJson.getProperties().get("seconds per point").as(Double.class));

        assertEquals(modelFromJson2.getProperties().get("levels").as(Integer.class),
                modelFromJson.getProperties().get("levels").as(Integer.class));

        assertEquals(modelFromJson2.getProperties().get("sharedName").as(String.class),
                modelFromJson.getProperties().get("sharedName").as(String.class));

        assertEquals(modelFromJson2.getProperties().get("networkMetadata").as(String.class),
                modelFromJson.getProperties().get("networkMetadata").as(String.class));

        assertEquals(modelFromJson2.getProperties().get("name").as(String.class),
                modelFromJson.getProperties().get("name").as(String.class));

        assertEquals(modelFromJson2.getProperties().get("suid").as(Integer.class),
                modelFromJson.getProperties().get("suid").as(Integer.class));

        assertEquals(modelFromJson2.getProperties().get("selected").as(Boolean.class),
                modelFromJson.getProperties().get("selected").as(Boolean.class));
    }
}