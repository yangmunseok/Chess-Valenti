package org.spring.createa.chessvalenti.util;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

@Converter
public class MapToJsonConverter implements AttributeConverter<Map, String> {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public String convertToDatabaseColumn(Map attribute) {
    try {
      return objectMapper.writeValueAsString(attribute);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Map convertToEntityAttribute(String dbData) {
    try {
      return objectMapper.readValue(dbData, Map.class);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
