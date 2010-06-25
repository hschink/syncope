/*
 *  Copyright 2010 sara.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.console.pages;

import org.apache.wicket.PageParameters;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;

/**
 *
 * @author sara
 */
public class WelcomePage extends WebPage {

    public WelcomePage(PageParameters parameters) {
        super(parameters);
        add(new BookmarkablePageLink("schema", Schema.class));

        add(new BookmarkablePageLink("users", Users.class));

        add(new BookmarkablePageLink("roles", Roles.class));

        add(new BookmarkablePageLink("resources", Resources.class));

        add(new BookmarkablePageLink("connectors", Connectors.class));

        add(new BookmarkablePageLink("report", Report.class));

        add(new BookmarkablePageLink("configuration", Configuration.class));

        add(new BookmarkablePageLink("logout", Logout.class));

    }
}
