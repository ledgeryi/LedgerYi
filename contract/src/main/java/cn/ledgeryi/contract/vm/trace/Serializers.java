/*
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */
package cn.ledgeryi.contract.vm.trace;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = "VM")
public final class Serializers {

  public static String serializeFieldsOnly(Object value, boolean pretty) {
    try {
      ObjectMapper mapper = createMapper(pretty);
      mapper.setVisibilityChecker(fieldsOnlyVisibilityChecker(mapper));

      return mapper.writeValueAsString(value);
    } catch (Exception e) {
      log.error("JSON serialization error: ", e);
      return "{}";
    }
  }

  private static VisibilityChecker<?> fieldsOnlyVisibilityChecker(ObjectMapper mapper) {
    return mapper.getSerializationConfig().getDefaultVisibilityChecker()
        .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
        .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
        .withIsGetterVisibility(JsonAutoDetect.Visibility.NONE);
  }

  public static ObjectMapper createMapper(boolean pretty) {
    ObjectMapper mapper = new ObjectMapper();
    if (pretty) {
      mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }
    return mapper;
  }
}
