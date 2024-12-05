/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fr.univparis1.tomcat;

import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;

import java.io.IOException;
import org.apache.catalina.Context;
import org.apache.catalina.Server;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

public class EarlySessionsUnloadListener implements LifecycleListener {

    private static final Log log = LogFactory.getLog(EarlySessionsUnloadListener.class);

    @Override
    public void lifecycleEvent(LifecycleEvent event) {

        if (!(event.getLifecycle() instanceof Server)) {
            throw new RuntimeException("" + EarlySessionsUnloadListener.class.getName() + "must be used in <Server>");
        }
        var server = (Server) event.getLifecycle();

        if (!Lifecycle.BEFORE_STOP_EVENT.equals(event.getType())) return;

        for (var service : server.findServices()) {
            for (var host : service.getContainer().findChildren()) {
                for (var context : host.findChildren()) {
                    var manager = ((Context) context).getManager();
                    if (!(manager instanceof EarlySessionsUnloadManager)) {
                        log.info("Skipping " + manager.getClass().getName() + " (expected EarlySessionsUnloadManager)");
                        continue;
                    }

                    var manager_ = (EarlySessionsUnloadManager) manager;
                    if (manager_.getActiveSessions() > 0) {
                        try {
                            log.info("Early saving sessions (" + context.getName() + ")");
                            manager_.early_unload();
                        } catch (IOException e) {
                            log.error(e);
                        }
                    } else {
                        log.debug("No need to save sessions (" + context.getName() + ")");
                    }
                }
            }
        }
    }

}
