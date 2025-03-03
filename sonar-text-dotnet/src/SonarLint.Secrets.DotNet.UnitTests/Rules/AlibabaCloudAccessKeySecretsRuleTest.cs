﻿/*
 * SonarAnalyzer for Text
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

// Ported from ...\sonar-secrets-plugin\src\test\java\com\sonarsource\secrets\rules\AlibabaCloudAccessKeySecretsRuleTest.java

using FluentAssertions;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using SonarLint.Secrets.DotNet.Rules;
using SonarLint.VisualStudio.Integration.UnitTests;
using System.Linq;
using static SonarLint.Secrets.DotNet.UnitTests.JavaTestFileUtils;
using static SonarLint.Secrets.DotNet.UnitTests.TestUtils;

namespace SonarLint.Secrets.DotNet.UnitTests.Rules
{
    [TestClass]
    public class AlibabaCloudAccessKeySecretsRuleTest
    {
        [TestMethod]
        public void MefCtor_CheckIsExported()
        {
            MefTestHelpers.CheckTypeCanBeImported<AlibabaCloudAccessKeySecretsRule, ISecretDetector>(null, null);
        }

        [TestMethod]
        public void TestRuleProperties()
        {
            var testSubject = new AlibabaCloudAccessKeySecretsRule();

            testSubject.RuleKey.Should().Be("secrets:S6336");
            testSubject.Name.Should().Be("Alibaba Cloud AccessKeys should not be disclosed");
            testSubject.Message.Should().Be("Make sure this Alibaba Cloud Access Key Secret is not disclosed.");
        }

        [TestMethod]
        public void testRuleRegex1Positive()
        {
            var testSubject = new AlibabaCloudAccessKeySecretsRule();

            var input = "String aliyunAccessKeySecret=\"KmkwlDrPBC68bgvZiNtrjonKIYmVT8\";";

            var secrets = testSubject.Find(input);

            secrets.Count().Should().Be(1);
            CheckExpectedSecretFound(input, "KmkwlDrPBC68bgvZiNtrjonKIYmVT8", secrets.First());
            CrossCheckWithJavaResult(input, new(1, 30, 1, 60), secrets.First());
        }

        [TestMethod]
        public void testRuleRegex2Positive()
        {
            var testSubject = new AlibabaCloudAccessKeySecretsRule();

            var input = "static string AccessKeySecret = \"l0GdwcDYdJwB1VJ5pv0ormyTV9nhvW \";";

            var secrets = testSubject.Find(input);

            secrets.Count().Should().Be(1);
            CheckExpectedSecretFound(input, "l0GdwcDYdJwB1VJ5pv0ormyTV9nhvW", secrets.First());
            CrossCheckWithJavaResult(input, new(1, 33, 1, 63), secrets.First());
        }

        [TestMethod]
        public void TestRuleRegexNegative()
        {
            var testSubject = new AlibabaCloudAccessKeySecretsRule();

            var secrets = testSubject.Find(readFileAndNormalize(Constants.RootPath + "checks\\GoogleCloudAccountKeyCheck\\GoogleCloudAccountNegative.json", UTF_8));

            secrets.Should().BeEmpty();
        }

        [TestMethod]
        public void testRuleRegexExamplePositive()
        {
            var testSubject = new AlibabaCloudAccessKeySecretsRule();

            var input = "String aliyunAccessKeySecret=\"KmkwlDrPBC68bgvZiNtrjonKIYmVT8\";";

            var secrets = testSubject.Find(input);

            secrets.Count().Should().Be(1);
            CheckExpectedSecretFound(input, "KmkwlDrPBC68bgvZiNtrjonKIYmVT8", secrets.First());
            CrossCheckWithJavaResult(input, new(1, 30, 1, 60), secrets.First());
        }

        [TestMethod]
        public void testRuleRegexExampleNegativeLowEntropy()
        {
            var testSubject = new AlibabaCloudAccessKeySecretsRule();

            var secrets = testSubject.Find("String aliyunAccessKeySecret=\"100000000000000000000000000000\";");

            secrets.Should().BeEmpty();
        }
    }
}
