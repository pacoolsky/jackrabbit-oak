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
package org.apache.jackrabbit.mongomk.prototype;

import java.util.Map;

import org.apache.jackrabbit.mk.json.JsopStream;

/**
 * Represents a node held in memory (in the cache for example).
 */
public class Node {

    final String path;
    final Revision rev;
    final Map<String, String> properties = Utils.newMap();
    
    Node(String path, Revision rev) {
        this.path = path;
        this.rev = rev;
    }
    
    void setProperty(String propertyName, String value) {
        properties.put(propertyName, value);
    }
    
    public String getProperty(String propertyName) {
        return properties.get(propertyName);
    }    
    
    public String toString() {
        StringBuilder buff = new StringBuilder();
        buff.append("path: ").append(path).append('\n');
        buff.append("rev: ").append(rev).append('\n');
        buff.append(properties);
        buff.append('\n');
        return buff.toString();
    }
    
    /**
     * Create an add node operation for this node.
     */
    UpdateOp asOperation(boolean isNew) {
        String id = convertPathToDocumentId(path);
        UpdateOp op = new UpdateOp(id, isNew);
        op.set("_id", id);
        if (!isNew) {
            op.increment("_changeCount", 1L);
        }
        for (String p : properties.keySet()) {
            op.addMapEntry(p, rev.toString(), properties.get(p));
        }
        return op;
    }

    static String convertPathToDocumentId(String path) {
        int depth = Utils.pathDepth(path);
        return depth + ":" + path;
    }

    public void append(JsopStream json) {
        json.object();
        for (String p : properties.keySet()) {
            json.key(p).encodedValue(properties.get(p));
        }
        json.endObject();
    }

}