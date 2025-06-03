package school.hei.asa.handler.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import school.hei.asa.PojaGenerated;

@PojaGenerated
public record ErrorModel(@JsonProperty("message") String message) {}
