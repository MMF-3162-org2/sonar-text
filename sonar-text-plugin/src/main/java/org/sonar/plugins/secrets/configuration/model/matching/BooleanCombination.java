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
package org.sonar.plugins.secrets.configuration.model.matching;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;
import org.sonar.plugins.secrets.configuration.deserialization.BooleanCombinationDeserializer;

@JsonDeserialize(using = BooleanCombinationDeserializer.class)
public class BooleanCombination implements Match {
  private BooleanCombinationType type;
  private List<Match> matches;

  public BooleanCombinationType getType() {
    return type;
  }

  public void setType(BooleanCombinationType type) {
    this.type = type;
  }

  public List<Match> getMatches() {
    return matches;
  }

  public void setMatches(List<Match> matches) {
    this.matches = matches;
  }
}
