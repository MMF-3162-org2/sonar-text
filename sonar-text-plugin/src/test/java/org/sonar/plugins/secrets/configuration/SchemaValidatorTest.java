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
package org.sonar.plugins.secrets.configuration;

import java.net.URL;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

class SchemaValidatorTest {

  @Test
  void specificationFilesAreValid() {
    assertThatNoException().isThrownBy(SchemaValidator::validateConfigurationFiles);
  }

  @ParameterizedTest
  @ValueSource(strings = {"validMinSpec.yaml", "validReferenceSpec.yaml"})
  void testSpecificationFilesAreValid(String specificationFileName) {
    URL specificationUrl = Thread.currentThread().getContextClassLoader().getResource("secretsConfiguration/" + specificationFileName);

    assertThatNoException().isThrownBy(() -> SchemaValidator.validate(specificationUrl));
  }

  @ParameterizedTest
  @ValueSource(strings = {"invalidEmptySpec.yaml", "invalidSpecMissingRequiredField.yaml", "invalidSpecWithUnexpectedField.yaml",
    "invalidSpecWithWrongType.yaml"})
  void testSpecificationFilesAreInValid(String specificationFileName) {
    URL specificationUrl = Thread.currentThread().getContextClassLoader().getResource("secretsConfiguration/" + specificationFileName);

    assertThatExceptionOfType(SchemaValidationException.class)
      .isThrownBy(() -> SchemaValidator.validate(specificationUrl))
      .withMessage(String.format("Specification file \"%s\" failed the schema validation", specificationFileName));
  }

  @Test
  void testSpecificationFilesAreInValid() {
    URL specificationUrl = Thread.currentThread().getContextClassLoader().getResource("doesNotExist.yaml");

    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> SchemaValidator.validate(specificationUrl));
  }
}
