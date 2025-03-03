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

// Ported from ...\sonar-secrets-plugin\src\test\java\com\sonarsource\secrets\EntropyCheckerTest.java

using Microsoft.VisualStudio.TestTools.UnitTesting;
using System.IO;
using static SonarLint.Secrets.DotNet.UnitTests.JavaAssertions;
using static SonarLint.Secrets.DotNet.UnitTests.JavaTestFileUtils;

namespace SonarLint.Secrets.DotNet.UnitTests
{
    [TestClass]
    public class EntropyCheckerTest
    {
        [TestMethod]
        public void computeSampleValue(){
            assertThat(EntropyChecker.CalculateShannonEntropy("0040878d3579659158d09ad09b6a9849d18e0e22")).isEqualTo(3.587326145256008);
        }

        [TestMethod]
        public void entropyCheckPositive() {
            assertThat(EntropyChecker.HasLowEntropy("06c6d5715a1ede6c51fc39ff67fd647f740b656d")).isTrue();
        }

        [TestMethod]
        public void entropyCheckNegative() {
            assertThat(EntropyChecker.HasLowEntropy("qAhEMdXy/MPwEuDlhh7O0AFBuzGvNy7AxpL3sX3q")).isFalse();
        }

        [TestMethod]
        public void thresholdSplitsFalsePositiveGoodEnough() {
            double falsePositivesAboveThreshold = ProcessFile(Constants.RootPath + "EntropyChecker\\false-positives.txt", EntropyChecker.ENTROPY_THRESHOLD);
            double truePositivesAboveThreshold = ProcessFile(Constants.RootPath + "EntropyChecker\\true-positives.txt", EntropyChecker.ENTROPY_THRESHOLD);

            // this assertions can be changed if we will get more data that, for example, will show more false positives
            // the goal of the test to fail if threshold value will be changed, since current value is the sweet spot on data we have so far
            assertThat(falsePositivesAboveThreshold).isLessThan(0.5);
            assertThat(truePositivesAboveThreshold).isEqualTo(100.0);
        }

        private double ProcessFile(string fileName, double threshold) {
            var javaTestFileName = LocateJavaTestFile(fileName);

            var lines = File.ReadAllLines(javaTestFileName);
            int aboveThreshold = 0;
            int total = 0;
            foreach (var line in lines)
            {
                total++;
                double entropy = EntropyChecker.CalculateShannonEntropy(line);
                if (entropy > threshold)
                {
                    aboveThreshold++;
                }
            }
            return 1.0 * aboveThreshold / total * 100;
        }
    }
}
