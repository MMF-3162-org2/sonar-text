/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2023 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.plugins.secrets.configuration.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import java.io.InputStream;
import java.util.Set;

public class SchemaValidator {

  private static final String FILE_LOCATION = "/org/sonar/plugins/secrets/configuration/specifications/";
  private static final String VALIDATION_SCHEMA_FILE = "specification-json-schema.json";
  private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory());
  private static final JsonSchema SPECIFICATION_VALIDATION_SCHEMA;

  private SchemaValidator() {
  }

  static {
    JsonSchemaFactory schemaFactory = JsonSchemaFactory
      .builder(JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012))
      .objectMapper(MAPPER)
      .build();
    InputStream validationSchema = SchemaValidator.class.getResourceAsStream(FILE_LOCATION + VALIDATION_SCHEMA_FILE);

    SPECIFICATION_VALIDATION_SCHEMA = schemaFactory.getSchema(validationSchema);
  }

  public static void validateSpecification(JsonNode specification, String fileName) {
    Set<ValidationMessage> validate = validate(specification);
    if (!validate.isEmpty()) {
      String errorMessage = String.format("Specification file \"%s\" failed the schema validation", fileName);
      throw new SchemaValidationException(errorMessage);
    }
  }

  public static Set<ValidationMessage> validate(JsonNode specification) {
    return SPECIFICATION_VALIDATION_SCHEMA.validate(specification);
  }
}
