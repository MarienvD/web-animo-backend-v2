package org.acme.domain;

import animo.core.model.Model;
import animo.exceptions.AnimoException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.annotation.Timed;
import org.acme.HeadlessMain;
import org.acme.ModelAnalyzer;
import org.acme.ModelMapper;
import org.acme.JobResult;
import org.acme.processors.SimulationResultDto;
import org.jboss.logging.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

public class SimulationJobProcessor implements JobProcessor {
    private final static Logger logger = Logger.getLogger(SimulationJobProcessor.class);
    private final SimulationJob params;
    private final String configFilePath;

    public SimulationJobProcessor(SimulationJob params, String configFilePath) {
        this.params = params;
        this.configFilePath = configFilePath;
    }

    @Timed(value = "simulation")
    public JobResult run() {
        logger.infof("Simulator %s is going to simulate", params.getId());
        Instant start = Instant.now();
        Model resultModel = null;
        JSONObject result;
        try {
            resultModel = ModelMapper.getModelFromJson(params.getModel(), params.getMinutesToSimulate());
            result = HeadlessMain.executeFromRequest(new ModelAnalyzer(configFilePath), new JSONObject(new ObjectMapper().writeValueAsString(params)), resultModel);
        } catch (JSONException | AnimoException | IOException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        Instant end = Instant.now();
        logger.infof("Simulation took %s", Duration.between(start, end));

        // filter disabled reactants
        try {
            SimulationResultDto simulationResultDto = new ObjectMapper().readValue(result.toString(), SimulationResultDto.class);
            simulationResultDto.filterDisabledReactants(params);
            return new JobResult(params.getId(), new ObjectMapper().writeValueAsString(simulationResultDto), params.getClientId(), JobType.SIMULATION);

        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

    }
}


