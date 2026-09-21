package com.recallhub.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AiResponsesClientTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void extractsAllStandardResponsesApiTextParts() throws Exception {
        var response = objectMapper.readTree("""
                {
                  "output": [
                    {"type":"reasoning","content":[]},
                    {"type":"message","content":[
                      {"type":"output_text","text":"第一段。"},
                      {"type":"output_text","text":"第二段。"}
                    ]}
                  ]
                }
                """);

        assertThat(AiResponsesClient.extractOutputText(response)).isEqualTo("第一段。\n第二段。");
    }

    @Test
    void supportsCompatibleApiTopLevelOutputText() throws Exception {
        var response = objectMapper.readTree("{\"output_text\":\"整理后的日记。\"}");

        assertThat(AiResponsesClient.extractOutputText(response)).isEqualTo("整理后的日记。");
    }
}
