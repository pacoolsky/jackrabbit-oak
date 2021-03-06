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
package org.apache.jackrabbit.mongomk.impl.action;

import org.apache.jackrabbit.mongomk.impl.MongoNodeStore;
import org.apache.jackrabbit.mongomk.impl.model.MongoCommit;

import com.mongodb.DBCollection;
import com.mongodb.WriteConcern;
import com.mongodb.WriteResult;

/**
 * An action for saving a commit.
 */
public class SaveCommitAction extends BaseAction<Boolean> {

    private final MongoCommit commitMongo;

    /**
     * Constructs a new {@code SaveCommitAction}.
     *
     * @param nodeStore Node store.
     * @param commitMongo The {@link MongoCommit} to save.
     */
    public SaveCommitAction(MongoNodeStore nodeStore, MongoCommit commitMongo) {
        super(nodeStore);
        this.commitMongo = commitMongo;
    }

    @Override
    public Boolean execute() throws Exception {
        DBCollection commitCollection = nodeStore.getCommitCollection();
        WriteResult writeResult = commitCollection.insert(commitMongo, WriteConcern.SAFE);
        if (writeResult.getError() != null) {
            throw new Exception(String.format("Insertion wasn't successful: %s", writeResult));
        }
        return Boolean.TRUE;
    }
}
