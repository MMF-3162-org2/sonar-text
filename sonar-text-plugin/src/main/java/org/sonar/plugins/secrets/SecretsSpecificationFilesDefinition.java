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
package org.sonar.plugins.secrets;

import java.util.Set;

public class SecretsSpecificationFilesDefinition {

  private SecretsSpecificationFilesDefinition() {

  }

  public static Set<String> existingSecretSpecifications() {
    return Set.of(
      "airtable.yaml",
      "alchemy.yaml",
      "alibaba.yaml",
      "artifactory.yaml",
      "aws.yaml",
      "azure.yaml",
      "clarifai.yaml",
      "digitalocean.yaml",
      "discord.yaml",
      "django.yaml",
      "docker-hub.yaml",
      "facebook.yaml",
      "figma.yaml",
      "firebase.yaml",
      "gcp.yaml",
      "github.yaml",
      "gitlab.yaml",
      "google-api.yaml",
      "google-oauth2.yaml",
      "google-recaptcha.yaml",
      "grafana.yaml",
      "hashicorp.yaml",
      "ibm.yaml",
      "infura.yaml",
      "mailgun.yaml",
      "mongodb.yaml",
      "ms-teams.yaml",
      "mws.yaml",
      "mysql.yaml",
      "npm.yaml",
      "odbc.yaml",
      "openweathermap.yaml",
      "planetscale.yaml",
      "postgresql.yaml",
      "postman.yaml",
      "pubkey-crypto.yaml",
      "pypi.yaml",
      "rabbitmq.yaml",
      "rapidapi.yaml",
      "redis.yaml",
      "riot.yaml",
      "sendgrid.yaml",
      "shippo.yaml",
      "shopify.yaml",
      "slack.yaml",
      "sonarqube.yaml",
      "spotify.yaml",
      "ssh.yaml",
      "stripe.yaml",
      "telegram.yaml",
      "typeform.yaml",
      "wakatime.yaml",
      "wechat.yaml",
      "yandex.yaml",
      "zapier.yaml",
      "zuplo.yaml");
  }
}
