package org.spring.createa.chessvalenti.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Map;
import org.spring.createa.chessvalenti.dto.game.GameResults;

@Converter
public class InsightDataConverter implements AttributeConverter<Map<String, GameResults>, String> {

  private final ObjectMapper objectMapper = new ObjectMapper()
      .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  @Override
  public String convertToDatabaseColumn(Map<String, GameResults> attribute) {
    try {
      return objectMapper.writeValueAsString(attribute);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Map<String, GameResults> convertToEntityAttribute(String dbData) {
    try {
      if (dbData == null || dbData.isEmpty()) {
        return null;
      }
      return objectMapper.readValue(dbData, new TypeReference<Map<String, GameResults>>() {
      });
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
