/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.console;

import org.junit.Test;

public class ConnInstanceTestITCase extends AbstractTest {

    @Test
    public void browseCreateModal() {
        selenium.click("css=img[alt=\"Resources\"]");

        selenium.waitForCondition("selenium.isElementPresent(\"//div[@id='tabs']\");", "30000");

        selenium.click("//div[@id='tabs']/ul/li[2]/a");

        selenium.click("//div[3]/div[2]/a");

        selenium.waitForCondition(
                "selenium.isElementPresent(\"//form/div[2]/div/div/div[3]/div[2]/span/select\");", "30000");

        selenium.waitForCondition("selenium.isElementPresent(\"//iframe\");", "30000");
        selenium.selectFrame("index=0");

        selenium.select("//select[@name='location:dropDownChoiceField']", "value=0");

        selenium.waitForCondition(
                "selenium.isElementPresent(\"//select[@name='connectorName:dropDownChoiceField']/option[3]\");",
                "30000");

        selenium.select("//select[@name='connectorName:dropDownChoiceField']", "label=org.connid.bundles.soap");

        selenium.click("//div[2]/form/div[2]/ul/li[2]/a/span");
        selenium.click("//div[2]/form/div[2]/ul/li[1]/a/span");

        assertTrue(selenium.isElementPresent("//form/div[2]/div/div/div[3]/div[2]/"));

        seleniumDriver.switchTo().defaultContent();

        selenium.click("css=a.w_close");
    }

    @Test
    public void browseEditModal() {
        selenium.click("css=img[alt=\"Resources\"]");

        selenium.waitForCondition("selenium.isElementPresent(\"//div[@id='tabs']\");", "30000");

        selenium.click("//div[3]/ul/li[2]/a");
        selenium.click("//tr[4]/td[7]/div/span[13]/a");

        selenium.waitForCondition(
                "selenium.isElementPresent(\"//div[2]/form/div[2]/div/div/div[3]/div[2]/span/select\");", "30000");

        selenium.waitForCondition("selenium.isElementPresent(\"//iframe\");", "30000");
        selenium.selectFrame("index=0");

        assertEquals("ConnInstance103", selenium.getAttribute("//input[@name='displayName:textField']/@value"));

        assertEquals("org.connid.bundles.soap",
                selenium.getSelectedLabel("//select[@name='connectorName:dropDownChoiceField']"));

        selenium.click("//div[2]/form/div[2]/ul/li[2]/a/span");

        seleniumDriver.switchTo().defaultContent();

        selenium.click("css=a.w_close");
    }

    @Test
    public void delete() {
        selenium.click("css=img[alt=\"Resources\"]");

        selenium.waitForCondition("selenium.isElementPresent(\"//div[@id='tabs']\");", "30000");

        selenium.click("//div[3]/ul/li[2]/a");
        selenium.click("//tr[4]/td[7]/div/span[15]/a");

        assertTrue(selenium.getConfirmation().equals("Do you really want to delete the selected item(s)?"));

        selenium.waitForCondition("selenium.isTextPresent(\"Error:\");", "10000");
    }

    @Test
    public void checkConnection() {
        selenium.click("css=img[alt=\"Resources\"]");

        selenium.waitForCondition("selenium.isElementPresent(\"//div[@id='tabs']\");", "30000");

        selenium.click("//div[3]/ul/li[2]/a");
        selenium.click("//tr[2]/td[7]/div/span[13]/a");

        selenium.waitForCondition(
                "selenium.isElementPresent(\"//select[@name='version:dropDownChoiceField']\");", "30000");

        selenium.waitForCondition("selenium.isElementPresent(\"//iframe\");", "30000");
        selenium.selectFrame("index=0");

        selenium.click("//div[2]/form/div[2]/ul/li[2]/a");

        selenium.waitForCondition(
                "selenium.isElementPresent(\"//div[2]/form/div[2]/div/div/div[3]/div[2]/span/select\");", "30000");

        selenium.click("//div[2]/form/div[2]/div[2]/div/span/div[2]/div[30]/a");

        selenium.waitForCondition(
                "selenium.isElementPresent(\"//div/ul/li/span[contains(text(),'Successful connection')]\");", "30000");

        seleniumDriver.switchTo().defaultContent();
    }

    @Test
    public void issueSyncope506() {
        selenium.click("css=img[alt=\"Resources\"]");

        selenium.waitForCondition("selenium.isElementPresent(\"//div[@id='tabs']\");", "30000");

        selenium.click("//tr[4]/td[3]/div/a/span");

        selenium.waitForCondition(
                "selenium.isElementPresent(\"//select[@name='version:dropDownChoiceField']\");", "30000");

        selenium.selectFrame("index=0");

        selenium.click("//div[2]/form/div[2]/ul/li[2]/a");

        selenium.waitForCondition(
                "selenium.isElementPresent(\"//div[2]/form/div[2]/div/div/div[3]/div[2]/span/select\");", "30000");

        selenium.click("//div[2]/form/div[2]/div[2]/div/span/div[2]/div[30]/div[3]/span/div/div/span/a[2]/span/span");

        selenium.click("//div[2]/form/div[3]/input");

        seleniumDriver.switchTo().defaultContent();

        selenium.waitForCondition("selenium.isTextPresent(\"Operation executed successfully\");", "30000");

        selenium.click("//tr[4]/td[3]/div/a/span");

        selenium.waitForCondition(
                "selenium.isElementPresent(\"//select[@name='version:dropDownChoiceField']\");", "30000");

        selenium.waitForCondition("selenium.isElementPresent(\"//iframe\");", "30000");
        selenium.selectFrame("index=0");

        selenium.click("//div[2]/form/div[2]/ul/li[2]/a");

        selenium.waitForCondition(
                "selenium.isElementPresent(\"//div[2]/form/div[2]/div/div/div[3]/div[2]/span/select\");", "30000");

        selenium.waitForCondition("selenium.isElementPresent(\"//input[@value='99']\");", "3000");

        selenium.click("//div[2]/form/div[2]/ul/li[2]/a/span");

        seleniumDriver.switchTo().defaultContent();

        selenium.click("css=a.w_close");
    }
}
