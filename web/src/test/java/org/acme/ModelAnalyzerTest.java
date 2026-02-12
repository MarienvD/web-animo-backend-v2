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
    void getModelFromJson() throws JSONException, AnimoException, IOException {
        InputStream is = ModelAnalyzerTest.class.getResourceAsStream("request2.json");
        CytoscapeModel job = new ObjectMapper().readValue(is, CytoscapeModel.class);
        JSONObject modelJson = new JSONObject(job);
        Model modelFromJson = ModelAnalyzer.getModelFromJson(modelJson, 1);
        assertNotNull(modelFromJson);
    }
}