/*
 * Copyright (C) 2012 Pixmob (http://github.com/pixmob)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.pixmob.hcl.demo.tasks;

import java.net.HttpURLConnection;

import org.pixmob.hcl.HttpClient;
import org.pixmob.hcl.HttpResponse;
import org.pixmob.hcl.HttpResponseHandler;
import org.pixmob.hcl.demo.R;
import org.pixmob.hcl.demo.Task;
import org.pixmob.hcl.demo.TaskExecutionFailedException;

import android.content.Context;

/**
 * {@link Task} implementation for testing redirection (status code 301).
 * @author Pixmob
 */
public class RedirectTask extends Task {
    public RedirectTask(final Context context) {
        super(context, R.string.task_redirect);
    }
    
    @Override
    protected void doRun() throws Exception {
        final HttpClient hc = createClient();
        hc.get("http://google.fr")
                .expectStatusCode(HttpURLConnection.HTTP_MOVED_PERM)
                .setHandler(new HttpResponseHandler() {
                    @Override
                    public void onResponse(HttpResponse response)
                            throws Exception {
                        if (!"text/html".equals(response.getContentType())) {
                            throw new TaskExecutionFailedException(
                                    "Expected content type text/html, got "
                                            + response.getContentType());
                        }
                        
                        if (!"http://www.google.fr/".equals(response
                                .getFirstHeaderValue("Location"))) {
                            throw new TaskExecutionFailedException(
                                    "Expected Location header set to www.google.fr/, got "
                                            + response
                                                    .getFirstHeaderValue("Location"));
                        }
                    }
                }).execute();
    }
}
